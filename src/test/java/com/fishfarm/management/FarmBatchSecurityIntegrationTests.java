package com.fishfarm.management;

import com.fasterxml.jackson.databind.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.security.FarmContext;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.business-zone=UTC","app.legacy-setup-token=integration-test-only-setup-token"})
@AutoConfigureMockMvc
class FarmBatchSecurityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    final Set<Long> farmIds=new HashSet<>(),accountIds=new HashSet<>();
    record Session(MockHttpSession session,JsonNode user) {}
    private static final java.util.concurrent.atomic.AtomicInteger IPS=new java.util.concurrent.atomic.AtomicInteger();
    private final String testIp="192.0.2."+IPS.incrementAndGet();
    String today(){return LocalDate.now().toString();}
    String key(){return UUID.randomUUID().toString();}
    Map<String,Object> data(Object... pairs){Map<String,Object> map=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]);return map;}
    MvcResult perform(Session s,String method,String path,Object body,int status) throws Exception {
        MockHttpServletRequestBuilder builder=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};
        builder.with(request -> { request.setRemoteAddr(testIp); return request; });
        if(s!=null)builder.session(s.session());
        if(!method.equals("GET"))builder.with(csrf());
        if(body!=null)builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(builder).andExpect(status().is(status)).andReturn();
    }
    JsonNode call(Session s,String method,String path,Object body) throws Exception {
        String raw=perform(s,method,path,body,200).getResponse().getContentAsString();return raw.isBlank()?json.nullNode():json.readTree(raw);
    }
    Session register() throws Exception {
        String username="test-"+key();
        MvcResult result=perform(null,"POST","/api/auth/register",data("username",username,"displayName","Test Owner","password","Integration-password-123","farmName","Test Farm","claimExisting",false),200);
        JsonNode user=json.readTree(result.getResponse().getContentAsString());farmIds.add(user.get("farmId").asLong());accountIds.add(user.get("id").asLong());
        return new Session((MockHttpSession)result.getRequest().getSession(false),user);
    }
    long pond(Session s,String number) throws Exception {return call(s,"POST","/api/ponds",data("pondNumber",number,"pondName",number)).get("id").asLong();}
    long fish(Session s) throws Exception {return call(s,"POST","/api/fish-types",data("name","Rohu")).get("id").asLong();}
    long feed(Session s) throws Exception {
        MvcResult result=perform(s,"POST","/api/feed-types",data("sizeMm",1,"name","Starter","ratePerKg",50,"active",true),201);
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    void configure(Session s,long pond,long type) throws Exception {call(s,"PUT","/api/pond-feed-configs/"+pond+"?date="+today(),data("feedTypeId",type,"morningQtyKg",2,"eveningQtyKg",2));}
    JsonNode batch(Session s,long pond,long fish) throws Exception {return call(s,"POST","/api/batches",data("code","B-"+key(),"name","Nursery","pondId",pond,"fishTypeId",fish,"date",today(),"count",100,"purchaseCost",1000));}
    long stage(JsonNode batch,int index){return batch.get("stages").get(index).get("id").asLong();}
    @AfterEach void cleanup(){
        FarmContext.clear();
        for(Long farm:farmIds){
            for(String table:List.of("stage_expenses","batch_movements","batch_stages","fish_batches","farm_audit_events","pond_assignments","daily_feeds","feeding_records","pond_feed_configs","weight_entries","weighing_sessions","dispatches","harvest_items","harvests","fish_stock_entries","inventory","customers","feed_types","fish_types","ponds"))
                jdbc.update("DELETE FROM "+table+" WHERE farm_id=?"+(table.equals("batch_stages")?" ORDER BY id DESC":""),farm);
            jdbc.update("DELETE FROM farm_members WHERE farm_id=?",farm);jdbc.update("DELETE FROM farms WHERE id=?",farm);
        }
        for(Long account:accountIds)jdbc.update("DELETE FROM app_accounts WHERE id=?",account);
    }

    @Test void credentialedCorsAllowsOnlyConfiguredUiOrigins() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin","http://localhost:4174").header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4174")).andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(options("/api/auth/me").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void anonymousAndCsrfAndPasswordRulesAreEnforced() throws Exception {
        mvc.perform(get("/api/ponds")).andExpect(status().isUnauthorized());
        Session owner=register();
        mvc.perform(post("/api/ponds").session(owner.session()).contentType(MediaType.APPLICATION_JSON).content("{\"pondNumber\":\"P1\",\"pondName\":\"No csrf\"}")).andExpect(status().isForbidden());
        perform(null,"POST","/api/auth/login",data("username",owner.user().get("username").asText(),"password","incorrect"),401);
        perform(null,"POST","/api/auth/register",data("username","weak-"+key(),"displayName","Weak","password","short","farmName","No","claimExisting",false),400);
        JsonNode csrfToken=call(null,"GET","/api/auth/csrf",null);assertThat(csrfToken.get("token").asText()).isNotBlank();
    }
    @Test void tenantIsolationCoversListsIdsWritesAndRelations() throws Exception {
        Session a=register(),b=register();long pa=pond(a,"P1"),pb=pond(b,"P1"),fa=fish(a);fish(b);feed(a);feed(b);
        JsonNode list=call(b,"GET","/api/ponds",null);assertThat(list).hasSize(1);assertThat(list.get(0).get("id").asLong()).isEqualTo(pb);
        perform(b,"GET","/api/ponds/"+pa,null,404);
        perform(b,"PUT","/api/ponds/"+pa,data("pondNumber","P1","pondName","HACK"),404);
        perform(b,"POST","/api/fish-stock",data("fishTypeId",fa,"pondId",pb,"stockDate",today(),"purchaseAmount",100),404);
        assertThat(call(a,"GET","/api/ponds/"+pa,null).get("pondName").asText()).isEqualTo("P1");
        JsonNode ba=batch(a,pa,fa);perform(b,"GET","/api/batches/"+ba.get("id").asLong(),null,404);
        perform(b,"POST","/api/batch-stages/"+stage(ba,0)+"/mortality",data("requestId",key(),"date",today(),"count",1,"reason","Denied"),404);
        assertThat(call(b,"GET","/api/batches",null)).isEmpty();
    }
    @Test void labourIsAssignedLimitedAndRevocable() throws Exception {
        Session owner=register();long p1=pond(owner,"P1"),p2=pond(owner,"P2"),feed=feed(owner);configure(owner,p1,feed);configure(owner,p2,feed);
        String username="labour-"+key();
        call(owner,"POST","/api/team",data("username",username,"displayName","Worker","password","Integration-password-123","role","LABOUR","pondIds",List.of(p1)));
        JsonNode member=call(owner,"GET","/api/team",null).findParents("username").stream().filter(n->n.get("username").asText().equals(username)).findFirst().orElseThrow();accountIds.add(member.get("accountId").asLong());
        MvcResult logged=perform(null,"POST","/api/auth/login",data("username",username,"password","Integration-password-123"),200);
        Session labour=new Session((MockHttpSession)logged.getRequest().getSession(false),json.readTree(logged.getResponse().getContentAsString()));
        JsonNode dashboard=call(labour,"GET","/api/feeding-dashboard?date="+today(),null);assertThat(dashboard.get("ponds")).hasSize(1);
        call(labour,"PUT","/api/feeding-dashboard/"+p1+"/slots/MORNING",data("date",today(),"completed",true));
        perform(labour,"PUT","/api/feeding-dashboard/"+p2+"/slots/MORNING",data("date",today(),"completed",true),403);
        perform(labour,"PUT","/api/feeding-dashboard/"+p1+"/slots/MORNING",data("date",today(),"completed",false),400);
        perform(labour,"GET","/api/batches",null,403);perform(labour,"GET","/api/team",null,403);
        call(owner,"PUT","/api/team/"+member.get("id").asLong(),data("role","LABOUR","active",false,"pondIds",List.of(p1)));
        perform(labour,"GET","/api/auth/me",null,401);
        JsonNode audit=call(owner,"GET","/api/audit",null);assertThat(audit.toString()).contains("Worker").contains("COMPLETE_FEED");
    }
    @Test void passwordChangeRevokesExistingSessionsAndHashesPasswords() throws Exception {
        Session owner=register();String username=owner.user().get("username").asText();
        MvcResult logged=perform(null,"POST","/api/auth/login",data("username",username,"password","Integration-password-123"),200);
        Session second=new Session((MockHttpSession)logged.getRequest().getSession(false),owner.user());
        call(owner,"POST","/api/auth/password",data("currentPassword","Integration-password-123","newPassword","Replacement-password-123"));
        perform(second,"GET","/api/auth/me",null,401);
        perform(null,"POST","/api/auth/login",data("username",username,"password","Integration-password-123"),401);
        perform(null,"POST","/api/auth/login",data("username",username,"password","Replacement-password-123"),200);
        String hash=jdbc.queryForObject("SELECT password_hash FROM app_accounts WHERE id=?",String.class,owner.user().get("id").asLong());assertThat(hash).startsWith("$2").doesNotContain("Replacement");
    }
    @Test void newFarmAndSelectionDoNotLeakPreviousFarmData() throws Exception {
        Session owner=register();pond(owner,"Old pond");
        JsonNode farms=call(owner,"POST","/api/auth/farms",data("name","Second Farm"));long id=farms.get(1).get("id").asLong();farmIds.add(id);
        call(owner,"POST","/api/auth/select-farm",data("farmId",id));assertThat(call(owner,"GET","/api/ponds",null)).isEmpty();
        call(owner,"POST","/api/auth/select-farm",data("farmId",owner.user().get("farmId").asLong()));assertThat(call(owner,"GET","/api/ponds",null)).hasSize(1);
        Session other=register();perform(other,"POST","/api/auth/select-farm",data("farmId",id),403);
    }
    @Test void existingFarmRequiresSetupCodeAndUnownedEntitiesAreRestricted() throws Exception {
        perform(null,"POST","/api/auth/register",data("username","claim-"+key(),"displayName","Claim","password","Integration-password-123","farmName","Existing","claimExisting",true,"setupToken","wrong"),403);
        for(var entity:emf.getMetamodel().getEntities())assertThat(FarmOwnedEntity.class.isAssignableFrom(entity.getJavaType()) || Set.of(Farm.class,AppAccount.class,FarmMember.class).contains(entity.getJavaType())).as(entity.getName()+" must be tenant scoped").isTrue();
    }
    @Test void fullTwoStageCostingKeepsMortalityAndFeedSnapshots() throws Exception {
        Session owner=register();long a=pond(owner,"Nursery"),b=pond(owner,"Grow-out"),fish=fish(owner),feed=feed(owner);configure(owner,a,feed);configure(owner,b,feed);
        JsonNode batch=batch(owner,a,fish);long batchId=batch.get("id").asLong(),s1=stage(batch,0);
        call(owner,"PUT","/api/feeding-dashboard/"+a+"/slots/MORNING",data("date",today(),"completed",true));
        call(owner,"POST","/api/batch-stages/"+s1+"/expenses",data("requestId",key(),"date",today(),"category","MEDICINE","amount",4900));
        call(owner,"POST","/api/batch-stages/"+s1+"/mortality",data("requestId",key(),"date",today(),"count",40,"reason","Nursery mortality"));
        Map<String,Object> transfer=data("requestId",key(),"date",today(),"count",60,"targetPondId",b,"transferCost",500);
        JsonNode result=call(owner,"POST","/api/batch-stages/"+s1+"/transfers",transfer);long s2=stage(result,1);
        JsonNode retry=call(owner,"POST","/api/batch-stages/"+s1+"/transfers",transfer);assertThat(retry.get("stages")).hasSize(2);
        assertThat(result.get("stages").get(1).get("openingCost").decimalValue()).isEqualByComparingTo("6000");
        perform(owner,"PUT","/api/feeding-dashboard/"+a+"/slots/MORNING",data("date",today(),"completed",false),400);
        call(owner,"PUT","/api/feeding-dashboard/"+b+"/slots/MORNING",data("date",today(),"completed",true));
        result=call(owner,"POST","/api/batch-stages/"+s2+"/expenses",data("requestId",key(),"date",today(),"category","LABOUR","amount",3400));
        assertThat(result.get("totalExpense").decimalValue()).isEqualByComparingTo("10000");assertThat(result.get("remainingCount").asInt()).isEqualTo(60);
        result=call(owner,"POST","/api/batch-stages/"+s2+"/mortality",data("requestId",key(),"date",today(),"count",10,"reason","Grow-out mortality"));
        assertThat(result.get("stages").get(1).get("costPerRemainingFish").decimalValue()).isEqualByComparingTo("200");
        Map<String,Object> harvest=data("requestId",key(),"date",today(),"count",50,"weightKg",25,"revenue",15000);
        result=call(owner,"POST","/api/batch-stages/"+s2+"/harvests",harvest);assertThat(result.get("closed").asBoolean()).isTrue();assertThat(result.get("finalProfit").decimalValue()).isEqualByComparingTo("5000");
        call(owner,"POST","/api/batch-stages/"+s2+"/harvests",harvest);
        assertThat(call(owner,"GET","/api/harvests",null)).hasSize(1);
        assertThat(call(owner,"GET","/api/inventory",null).get(0).get("currentStockKg").decimalValue()).isEqualByComparingTo("25");
        assertThat(call(owner,"GET","/api/batches/"+batchId,null).get("deadCount").asInt()).isEqualTo(50);
        Map<String,Object> settlement=data("requestId",key(),"date",today(),"amount",500,"notes","Final balance received");
        result=call(owner,"POST","/api/batch-stages/"+s2+"/settlements",settlement);assertThat(result.get("finalProfit").decimalValue()).isEqualByComparingTo("5500");
        assertThat(result.get("finalCostPerKg").decimalValue()).isEqualByComparingTo("400");
        call(owner,"POST","/api/batch-stages/"+s2+"/settlements",settlement);
        assertThat(call(owner,"GET","/api/batches/"+batchId,null).get("revenue").decimalValue()).isEqualByComparingTo("15500");
    }
    @Test void partialTransfersAllocateCostOnceAndRejectOverdrawAndOccupiedPonds() throws Exception {
        Session owner=register();long a=pond(owner,"A"),b=pond(owner,"B"),fish=fish(owner);JsonNode batch=batch(owner,a,fish);long s1=stage(batch,0);
        call(owner,"POST","/api/batch-stages/"+s1+"/mortality",data("requestId",key(),"date",today(),"count",40,"reason","Loss"));
        Map<String,Object> transfer=data("requestId",key(),"date",today(),"count",30,"targetPondId",b,"transferCost",50);
        JsonNode result=call(owner,"POST","/api/batch-stages/"+s1+"/transfers",transfer);
        assertThat(result.get("remainingCount").asInt()).isEqualTo(60);assertThat(result.get("totalExpense").decimalValue()).isEqualByComparingTo("1050");
        assertThat(result.get("stages").get(0).get("remainingCost").decimalValue()).isEqualByComparingTo("500");
        assertThat(result.get("stages").get(1).get("remainingCost").decimalValue()).isEqualByComparingTo("550");
        perform(owner,"POST","/api/batch-stages/"+s1+"/transfers",data("requestId",key(),"date",today(),"count",1,"targetPondId",b,"transferCost",0),400);
        perform(owner,"POST","/api/batch-stages/"+s1+"/mortality",data("requestId",key(),"date",today(),"count",31,"reason","Invalid"),400);
        transfer.put("count",1);perform(owner,"POST","/api/batch-stages/"+s1+"/transfers",transfer,409);
    }
    @Test void concurrentTransferRetryMovesFishOnlyOnce() throws Exception {
        Session owner=register();long a=pond(owner,"A"),b=pond(owner,"B"),fish=fish(owner);JsonNode batch=batch(owner,a,fish);long stage=stage(batch,0);
        MvcResult logged=perform(null,"POST","/api/auth/login",data("username",owner.user().get("username").asText(),"password","Integration-password-123"),200);
        Session other=new Session((MockHttpSession)logged.getRequest().getSession(false),owner.user());
        Map<String,Object> body=data("requestId",key(),"date",today(),"count",100,"targetPondId",b,"transferCost",50);
        ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Future<JsonNode> first=executor.submit(()->{start.await();return call(owner,"POST","/api/batch-stages/"+stage+"/transfers",body);});
            Future<JsonNode> second=executor.submit(()->{start.await();return call(other,"POST","/api/batch-stages/"+stage+"/transfers",body);});
            start.countDown();first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);
            JsonNode result=call(owner,"GET","/api/batches/"+batch.get("id").asLong(),null);assertThat(result.get("stages")).hasSize(2);assertThat(result.get("totalExpense").decimalValue()).isEqualByComparingTo("1050");
        } finally {executor.shutdownNow();}
    }
    @Test void invalidLoginAttemptsAreRateLimited() throws Exception {
        String username="missing-"+key();
        for(int i=0;i<10;i++)perform(null,"POST","/api/auth/login",data("username",username,"password","Wrong-password-123"),401);
        perform(null,"POST","/api/auth/login",data("username",username,"password","Wrong-password-123"),429);
    }

    @Test void existingStockEntryIsLinkedWithoutDuplicatingItsPurchase() throws Exception {
        Session owner=register();long pond=pond(owner,"A"),fish=fish(owner);
        long stockId=call(owner,"POST","/api/fish-stock",data("fishTypeId",fish,"pondId",pond,"stockDate",today(),"quantity",100,"purchaseAmount",300)).get("id").asLong();
        Map<String,Object> body=data("code","B-"+key(),"name","Existing stock","stockEntryId",stockId,"date",today(),"count",100,"purchaseCost",0);
        JsonNode batch=call(owner,"POST","/api/batches",body);assertThat(batch.get("purchaseCost").decimalValue()).isEqualByComparingTo("300");assertThat(call(owner,"GET","/api/fish-stock",null)).hasSize(1);
        body.put("code","Other-"+key());perform(owner,"POST","/api/batches",body,400);
        perform(owner,"DELETE","/api/fish-stock/"+stockId,null,400);
    }
    @Test void retroactiveEventsCannotChangeAllocatedCostHistory() throws Exception {
        Session owner=register();long pond=pond(owner,"A"),fish=fish(owner);JsonNode batch=batch(owner,pond,fish);long stage=stage(batch,0);
        perform(owner,"POST","/api/batch-stages/"+stage+"/expenses",data("requestId",key(),"date",LocalDate.now().minusDays(1).toString(),"category","OTHER","amount",10),400);
        perform(owner,"POST","/api/batch-stages/"+stage+"/mortality",data("requestId",key(),"date",LocalDate.now().plusDays(1).toString(),"count",1,"reason","Future"),400);
        assertThat(call(owner,"GET","/api/batches/"+batch.get("id").asLong(),null).get("remainingCount").asInt()).isEqualTo(100);
    }
    @Test void manualHarvestCannotBypassActiveBatchCounts() throws Exception {
        Session owner=register();long pond=pond(owner,"A"),fish=fish(owner);batch(owner,pond,fish);
        perform(owner,"POST","/api/fish-stock",data("fishTypeId",fish,"pondId",pond,"stockDate",today(),"quantity",10,"purchaseAmount",100),400);
        perform(owner,"DELETE","/api/ponds/"+pond,null,400);
        perform(owner,"POST","/api/harvests",data("pondId",pond,"harvestDate",today(),"items",List.of(data("fishTypeId",fish,"weightKg",1))),400);
    }
}
