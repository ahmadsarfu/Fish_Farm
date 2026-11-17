package com.fishfarm.management;

import com.fasterxml.jackson.databind.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.business-zone=UTC"})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FeedingDashboardIntegrationTests extends LegacyDomainTestContext {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PondRepository ponds;
    @Autowired FeedTypeRepository types;
    @Autowired DailyFeedRepository feeds;
    @Autowired AuditEventRepository events;
    @Autowired PondFeedConfigRepository configs;
    Pond pond;
    FeedType type;

    @BeforeEach void prepare() {
        pond = new Pond(); pond.setPondName("Checklist pond"); pond.setPondNumber(UUID.randomUUID().toString().substring(0,16));
        pond = ponds.saveAndFlush(pond);
        type = new FeedType(); type.setName("Checklist feed"); type.setSizeMm(1); type.setRatePerKg(new BigDecimal("50.25"));
        type = types.saveAndFlush(type);
    }
    void configure(String morning, String evening) throws Exception {
        mvc.perform(put("/api/pond-feed-configs/{id}", pond.getId()).param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "feedTypeId", type.getId(), "morningQtyKg", morning, "eveningQtyKg", evening))))
                .andExpect(status().isOk());
    }
    JsonNode tick(String time, boolean completed, LocalDate date) throws Exception {
        String result = mvc.perform(put("/api/feeding-dashboard/{id}/slots/{time}", pond.getId(), time)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("date", date.toString(), "completed", completed))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result);
    }

    @Test void configAndOneTapSaveBothSlotsWithExactExpense() throws Exception {
        configure("2.555", "3");
        JsonNode am = tick("MORNING", true, LocalDate.now());
        assertThat(am.get("completedSlots").asInt()).isEqualTo(1);
        assertThat(am.get("plannedSlots").asInt()).isEqualTo(2);
        assertThat(am.get("totalExpense").decimalValue()).isEqualByComparingTo("128.39");
        JsonNode pm = tick("EVENING", true, LocalDate.now());
        assertThat(pm.get("completedSlots").asInt()).isEqualTo(2);
        assertThat(pm.get("totalExpense").decimalValue()).isEqualByComparingTo("279.14");
        mvc.perform(get("/api/feeding-dashboard").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedSlots").value(2));
    }

    @Test void repeatedCompletionIsIdempotentAndKeepsSnapshots() throws Exception {
        configure("2", "2"); tick("MORNING", true, LocalDate.now());
        type.setRatePerKg(new BigDecimal("60")); types.saveAndFlush(type); configure("4", "5");
        JsonNode retry = tick("MORNING", true, LocalDate.now());
        assertThat(retry.get("totalExpense").decimalValue()).isEqualByComparingTo("100.50");
        assertThat(feeds.count()).isEqualTo(1);
        DailyFeed saved = feeds.findAll().get(0);
        assertThat(saved.getQuantityKg()).isEqualByComparingTo("2");
        assertThat(saved.getRatePerKg()).isEqualByComparingTo("50.25");
        JsonNode pm = tick("EVENING", true, LocalDate.now());
        assertThat(pm.get("totalExpense").decimalValue()).isEqualByComparingTo("400.50");
    }

    @Test void undoIsIdempotentAndRestoresTotals() throws Exception {
        configure("2", "2"); tick("MORNING", true, LocalDate.now());
        JsonNode undo = tick("MORNING", false, LocalDate.now());
        assertThat(undo.get("completedSlots").asInt()).isZero();
        assertThat(undo.get("totalExpense").decimalValue()).isEqualByComparingTo("0");
        tick("MORNING", false, LocalDate.now()); assertThat(feeds.count()).isZero();
    }

    @Test void datesAreIsolatedAndFutureCompletionIsRejected() throws Exception {
        configure("2", "2"); tick("MORNING", true, LocalDate.now().minusDays(1));
        mvc.perform(get("/api/feeding-dashboard").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedSlots").value(0));
        mvc.perform(put("/api/feeding-dashboard/{id}/slots/MORNING", pond.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("date", LocalDate.now().plusDays(1).toString(), "completed", true))))
                .andExpect(status().isBadRequest());
    }

    @Test void missingSetupAndInactiveFeedCannotBeTicked() throws Exception {
        String body = json.writeValueAsString(Map.of("date", LocalDate.now().toString(), "completed", true));
        mvc.perform(put("/api/feeding-dashboard/{id}/slots/MORNING", pond.getId()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        configure("2", "2"); type.setActive(false); types.saveAndFlush(type);
        mvc.perform(put("/api/feeding-dashboard/{id}/slots/MORNING", pond.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("This feed is inactive. Update pond setup first"));
        assertThat(feeds.count()).isZero();
    }

    @Test void disabledSlotsAndInvalidQuantitiesAreHandled() throws Exception {
        configure("0", "2");
        mvc.perform(get("/api/feeding-dashboard").param("date", LocalDate.now().toString())).andExpect(jsonPath("$.plannedSlots").value(1));
        for (String quantity : List.of("-1", "0.0001")) {
            mvc.perform(put("/api/pond-feed-configs/{id}", pond.getId()).param("date", LocalDate.now().toString())
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("feedTypeId", type.getId(), "morningQtyKg", quantity, "eveningQtyKg", "2"))))
                    .andExpect(status().isBadRequest());
        }
        String body = json.writeValueAsString(Map.of("date", LocalDate.now().toString(), "completed", true));
        mvc.perform(put("/api/feeding-dashboard/{id}/slots/MORNING", pond.getId()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test void manualRecordAlreadyMarksSlotCompleteWithoutDuplicate() throws Exception {
        configure("2", "2");
        DailyFeed manual = new DailyFeed(); manual.setPond(pond); manual.setFeedType(type); manual.setFeedingDate(LocalDate.now());
        manual.setFeedingTime(com.fishfarm.management.enums.FeedingTime.MORNING); manual.setQuantityKg(new BigDecimal("3"));
        manual.setRatePerKg(new BigDecimal("40")); manual.setExpense(new BigDecimal("120")); feeds.saveAndFlush(manual);
        JsonNode result = tick("MORNING", true, LocalDate.now());
        assertThat(result.get("totalExpense").decimalValue()).isEqualByComparingTo("120"); assertThat(feeds.count()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentTapsDoNotCreateDuplicateEntries() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            configure("2", "2");
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            Callable<Integer> action = () -> {
                com.fishfarm.management.security.FarmContext.set(1L,1L,"OWNER");
                ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try { return mvc.perform(put("/api/feeding-dashboard/{id}/slots/MORNING", pond.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("date", LocalDate.now().toString(), "completed", true))))
                        .andReturn().getResponse().getStatus(); } finally { com.fishfarm.management.security.FarmContext.clear(); }
            };
            Future<Integer> first = executor.submit(action), second = executor.submit(action);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(feeds.search(LocalDate.now(), LocalDate.now(), pond.getId())).hasSize(1);
        } finally {
            executor.shutdownNow();
            var ownedFeeds=feeds.search(LocalDate.now(), LocalDate.now(), pond.getId());
            var ids=ownedFeeds.stream().map(com.fishfarm.management.entity.DailyFeed::getId).toList();
            events.deleteAll(events.findAll().stream().filter(e->"DAILY_FEED".equals(e.getEntityType()) && ids.contains(e.getEntityId())).toList());
            feeds.deleteAll(ownedFeeds);
            configs.findByPondId(pond.getId()).ifPresent(configs::delete);
            ponds.deleteById(pond.getId()); types.deleteById(type.getId());
        }
    }
}
