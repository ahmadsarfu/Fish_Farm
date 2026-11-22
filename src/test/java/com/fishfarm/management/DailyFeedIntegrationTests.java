package com.fishfarm.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.enums.PondStatus;
import com.fishfarm.management.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.business-zone=UTC"})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class DailyFeedIntegrationTests extends LegacyDomainTestContext {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PondRepository ponds;
    @Autowired FeedTypeRepository types;
    @Autowired DailyFeedRepository feeds;
    Pond pond;
    FeedType type;

    @BeforeEach void prepare() {
        pond = new Pond();
        pond.setPondName("Feed test pond");
        pond.setPondNumber(UUID.randomUUID().toString().substring(0, 16));
        pond = ponds.saveAndFlush(pond);
        type = new FeedType(); type.setSizeMm(1); type.setName("Starter"); type.setRatePerKg(new BigDecimal("50.25"));
        type = types.saveAndFlush(type);
    }

    Map<String, Object> body() {
        Map<String, Object> body = new HashMap<>();
        body.put("pondId", pond.getId()); body.put("feedTypeId", type.getId());
        body.put("feedingDate", LocalDate.now().toString()); body.put("feedingTime", "MORNING");
        body.put("quantityKg", "2.555"); body.put("notes", "Missed feeding entered after review");
        return body;
    }

    long create(Map<String, Object> body) throws Exception {
        String result = mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }

    @Test void calculatesDecimalExpenseAndSummary() throws Exception {
        long id = create(body());
        assertThat(feeds.findById(id).orElseThrow().getExpense()).isEqualByComparingTo("128.39");
        mvc.perform(get("/api/daily-feeds/summary").param("from", LocalDate.now().toString()).param("to", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.totalQuantityKg").value(2.555)).andExpect(jsonPath("$.totalExpense").value(128.39));
    }

    @Test void masterRateChangesPreserveHistoricalRateOnEdit() throws Exception {
        long id = create(body());
        mvc.perform(put("/api/feed-types/{id}", type.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sizeMm\":1,\"name\":\"Starter\",\"ratePerKg\":60,\"active\":true}"))
                .andExpect(status().isOk());
        Map<String, Object> body = body(); body.put("quantityKg", "3.000");
        mvc.perform(post("/api/daily-feeds/{id}/review", id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("action","CORRECT","expectedVersion",0,"quantityKg","3.000","reason","Actual quantity was three kg"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ratePerKg").value(50.25)).andExpect(jsonPath("$.expense").value(150.75));
        body.put("feedingTime", "EVENING");
        long eveningId = create(body);
        assertThat(feeds.findById(eveningId).orElseThrow().getExpense()).isEqualByComparingTo("180.00");
    }

    @Test void duplicateEntriesAreRejectedButEveningIsSeparate() throws Exception {
        create(body());
        mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
        Map<String, Object> evening = body(); evening.put("feedingTime", "EVENING"); create(evening);
        assertThat(feeds.count()).isEqualTo(2);
    }

    @Test void rejectsInvalidQuantitiesDatesAndReferences() throws Exception {
        for (Object quantity : List.of("0", "-1", "0.0001", "10000000")) {
            Map<String, Object> body = body(); body.put("quantityKg", quantity);
            mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        Map<String, Object> future = body(); future.put("feedingDate", LocalDate.now().plusDays(1).toString());
        mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(future))).andExpect(status().isBadRequest());
        Map<String, Object> missing = body(); missing.put("pondId", Long.MAX_VALUE);
        mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(missing))).andExpect(status().isNotFound());
        assertThat(feeds.count()).isZero();
    }

    @Test void inactiveMastersAndPondsCannotBeUsedForNewEntries() throws Exception {
        type.setActive(false); types.saveAndFlush(type);
        mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body()))).andExpect(status().isBadRequest());
        type.setActive(true); types.saveAndFlush(type);
        pond.setStatus(PondStatus.INACTIVE); ponds.saveAndFlush(pond);
        mvc.perform(post("/api/daily-feeds").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body()))).andExpect(status().isBadRequest());
    }

    @Test void filtersByPondAndInclusiveDateAndRejectsReversedRange() throws Exception {
        create(body());
        String today = LocalDate.now().toString();
        mvc.perform(get("/api/daily-feeds").param("from", today).param("to", today).param("pondId", pond.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/daily-feeds").param("from", today).param("to", today).param("pondId", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/daily-feeds").param("from", LocalDate.now().plusDays(1).toString()).param("to", today)).andExpect(status().isBadRequest());
    }

    @Test void cancellationPreservesRecordAndOriginalButExcludesExpense() throws Exception {
        long id=create(body());
        mvc.perform(post("/api/daily-feeds/{id}/review",id).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("action","CANCEL","expectedVersion",0,"reason","Mistaken feeding entry"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.netExpense").value(0));
        assertThat(feeds.count()).isEqualTo(1);
        assertThat(feeds.findById(id).orElseThrow().getOriginalExpense()).isEqualByComparingTo("128.39");
        mvc.perform(delete("/api/daily-feeds/{id}",id)).andExpect(status().isConflict());
    }

    @Test void feedMasterRestrictsSizeDuplicatesAndRate() throws Exception {
        mvc.perform(post("/api/feed-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sizeMm\":1,\"name\":\"Duplicate\",\"ratePerKg\":10,\"active\":true}")).andExpect(status().isConflict());
        for (String body : List.of(
                "{\"sizeMm\":5,\"name\":\"Invalid\",\"ratePerKg\":10,\"active\":true}",
                "{\"sizeMm\":2,\"name\":\"Invalid\",\"ratePerKg\":0,\"active\":true}")) {
            mvc.perform(post("/api/feed-types").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/feed-types/{id}", type.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sizeMm\":2,\"name\":\"Changed\",\"ratePerKg\":10,\"active\":true}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/feed-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sizeMm\":2,\"name\":\"Grower\",\"ratePerKg\":42.50,\"active\":true}")).andExpect(status().isCreated());
    }
}
