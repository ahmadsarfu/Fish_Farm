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
class MedicineInventoryIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    final Set<Long> farmIds=new HashSet<>(),accountIds=new HashSet<>();
    record Session(MockHttpSession session,JsonNode user) {}
    private static final java.util.concurrent.atomic.AtomicLong PHONES=new java.util.concurrent.atomic.AtomicLong(9000000000L);
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
        MvcResult result=perform(null,"POST","/api/auth/register",data("username",username,"mobile",String.valueOf(PHONES.getAndIncrement()),"displayName","Test Owner","password","Integration-password-123","farmName","Test Farm","claimExisting",false),200);
        JsonNode user=json.readTree(result.getResponse().getContentAsString());farmIds.add(user.get("farmId").asLong());accountIds.add(user.get("id").asLong());
        return new Session((MockHttpSession)result.getRequest().getSession(false),user);
    }

    @AfterEach void cleanup(){FarmContext.clear();for(Long farm:farmIds){for(String table:List.of("medicine_movements","medicine_lots","medicine_products","farm_audit_events","farm_members"))jdbc.update("DELETE FROM "+table+" WHERE farm_id=?"+(table.equals("medicine_movements")?" ORDER BY id DESC":""),farm);jdbc.update("DELETE FROM farms WHERE id=?",farm);}for(Long account:accountIds)jdbc.update("DELETE FROM app_accounts WHERE id=?",account);}
    long product(Session s) throws Exception{return call(s,"POST","/api/medicines/products",data("name","Aqua Care","brand","Aqua","packLabel","1 L","unit","BOTTLE","minimumStock",5,"sellingPrice",35,"active",true)).get("id").asLong();}
    Map<String,Object> purchaseBody(long product,int quantity){return data("requestKey",key(),"productId",product,"batchNumber","LOT-A","expiryDate",LocalDate.now().plusDays(90).toString(),"date",today(),"quantity",quantity,"unitCost",20,"supplier","Supplier A","notes","Opening stock");}
    JsonNode purchase(Session s,long product,int quantity)throws Exception{return call(s,"POST","/api/medicines/purchases",purchaseBody(product,quantity));}
    Map<String,Object> moveBody(long lot,String kind,int qty){return data("requestKey",key(),"lotId",lot,"kind",kind,"date",today(),"quantity",qty,"unitPrice",35,"notes","Actual recorded movement");}
    JsonNode move(Session s,long lot,String kind,int qty)throws Exception{return call(s,"POST","/api/medicines/movements",moveBody(lot,kind,qty));}
    JsonNode totals(Session s)throws Exception{return call(s,"GET","/api/medicines/summary?from="+today()+"&to="+today(),null);}
    int stock(Session s)throws Exception{return call(s,"GET","/api/medicines/lots",null).get(0).get("stock").asInt();}
    void reverse(Session s,JsonNode m)throws Exception{call(s,"POST","/api/medicines/movements/"+m.get("id").asLong()+"/reverse",data("requestKey",key(),"reason","Owner verified correction"));}
    @Test void purchasesSalesOwnUseAndReturnsKeepSeparateLedgerAndFinancials()throws Exception{
        Session s=register();long p=product(s),lot=purchase(s,p,100).get("lotId").asLong();
        var sale=move(s,lot,"SALE",30);var use=move(s,lot,"OWN_USE",10);
        assertThat(stock(s)).isEqualTo(60);var t=totals(s);assertThat(t.get("sales").decimalValue()).isEqualByComparingTo("1050");assertThat(t.get("salesCost").decimalValue()).isEqualByComparingTo("600");assertThat(t.get("grossProfit").decimalValue()).isEqualByComparingTo("450");assertThat(t.get("ownUseCost").decimalValue()).isEqualByComparingTo("200");
        var returnBody=moveBody(lot,"SALES_RETURN",5);returnBody.put("referenceId",sale.get("id").asLong());var returned=call(s,"POST","/api/medicines/movements",returnBody);
        assertThat(stock(s)).isEqualTo(65);assertThat(totals(s).get("sales").decimalValue()).isEqualByComparingTo("875");
        reverse(s,returned);assertThat(stock(s)).isEqualTo(60);
        // A reversed return does not consume the remaining return allowance.
        var fullReturn=moveBody(lot,"SALES_RETURN",30);fullReturn.put("referenceId",sale.get("id").asLong());var all=call(s,"POST","/api/medicines/movements",fullReturn);
        assertThat(stock(s)).isEqualTo(90);assertThat(totals(s).get("sales").decimalValue()).isEqualByComparingTo("0");
        perform(s,"POST","/api/medicines/movements/"+sale.get("id").asLong()+"/reverse",data("requestKey",key(),"reason","Cannot reverse before returns"),400);
        reverse(s,all);reverse(s,sale);reverse(s,use);assertThat(stock(s)).isEqualTo(100);
        assertThat(totals(s).get("sales").decimalValue()).isEqualByComparingTo("0");assertThat(totals(s).get("ownUseCost").decimalValue()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medicine_movements WHERE farm_id=?",Integer.class,s.user().get("farmId").asLong())).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stage_expenses WHERE farm_id=?",Integer.class,s.user().get("farmId").asLong())).isZero();
        assertThat(call(s,"GET","/api/medicines/export?from="+today()+"&to="+today(),null).get("items")).hasSize(9);
    }
    @Test void duplicateRequestsAreIdempotentAndFractionalOrOversoldStockIsRejected()throws Exception{
        Session s=register();long p=product(s);var body=purchaseBody(p,2);var first=call(s,"POST","/api/medicines/purchases",body);assertThat(call(s,"POST","/api/medicines/purchases",body).get("id")).isEqualTo(first.get("id"));
        body.put("quantity",3);perform(s,"POST","/api/medicines/purchases",body,409);
        long lot=first.get("lotId").asLong();var saleBody=moveBody(lot,"SALE",1);var sale=call(s,"POST","/api/medicines/movements",saleBody);assertThat(call(s,"POST","/api/medicines/movements",saleBody).get("id")).isEqualTo(sale.get("id"));assertThat(stock(s)).isEqualTo(1);
        perform(s,"POST","/api/medicines/movements",moveBody(lot,"SALE",2),409);
        saleBody.put("requestKey",key());saleBody.put("quantity",1.5);perform(s,"POST","/api/medicines/movements",saleBody,400);
        saleBody.put("quantity",0);perform(s,"POST","/api/medicines/movements",saleBody,400);
        var returned=moveBody(lot,"SALES_RETURN",2);returned.put("referenceId",sale.get("id").asLong());perform(s,"POST","/api/medicines/movements",returned,400);
        assertThat(stock(s)).isEqualTo(1);
        var patch=data("name","Renamed identity","brand","Aqua","packLabel","1 L","unit","BOTTLE","minimumStock",5,"sellingPrice",99,"active",true,"expectedVersion",0);
        perform(s,"PUT","/api/medicines/products/"+p,patch,400);patch.put("name","Aqua Care");call(s,"PUT","/api/medicines/products/"+p,patch);
        assertThat(call(s,"GET","/api/medicines/history?from="+today()+"&to="+today(),null).get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("35");
    }
    @Test void expiredMedicineCannotBeSoldOrUsedButCanBeWrittenOff()throws Exception{
        Session s=register();long p=product(s);var body=purchaseBody(p,5);body.put("date",LocalDate.now().minusDays(60).toString());body.put("expiryDate",LocalDate.now().minusDays(1).toString());long lot=call(s,"POST","/api/medicines/purchases",body).get("lotId").asLong();
        perform(s,"POST","/api/medicines/movements",moveBody(lot,"SALE",1),400);perform(s,"POST","/api/medicines/movements",moveBody(lot,"OWN_USE",1),400);
        var damaged=move(s,lot,"DAMAGE",2);assertThat(stock(s)).isEqualTo(3);assertThat(totals(s).get("damageCost").decimalValue()).isEqualByComparingTo("40");reverse(s,damaged);assertThat(stock(s)).isEqualTo(5);
        move(s,lot,"SUPPLIER_RETURN",5);assertThat(stock(s)).isZero();
    }
    @Test void concurrentSalesCannotOversellTheSameLot()throws Exception{
        Session s=register();long lot=purchase(s,product(s),1).get("lotId").asLong();var pool=Executors.newFixedThreadPool(2);
        try {Callable<Integer> task=()->mvc.perform(post("/api/medicines/movements").session(s.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(moveBody(lot,"SALE",1)))).andReturn().getResponse().getStatus();var a=pool.submit(task);var b=pool.submit(task);assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}assertThat(stock(s)).isZero();
    }
    @Test void tenantAndRoleBoundariesProtectMedicineInventory()throws Exception{
        Session owner=register(),other=register();long lot=purchase(owner,product(owner),4).get("lotId").asLong();
        assertThat(call(other,"GET","/api/medicines/lots",null)).isEmpty();perform(other,"POST","/api/medicines/movements",moveBody(lot,"SALE",1),404);
        var m=move(owner,lot,"SALE",1);perform(other,"POST","/api/medicines/movements/"+m.get("id").asLong()+"/reverse",data("requestKey",key(),"reason","Other farm cannot reverse"),404);
        for(String role:List.of("MANAGER","LABOUR")){
            String username="med-"+key();call(owner,"POST","/api/team",data("username",username,"displayName",role,"password","Integration-password-123","role",role,"pondIds",List.of()));
            var logged=perform(null,"POST","/api/auth/login",data("username",username,"password","Integration-password-123"),200);var user=json.readTree(logged.getResponse().getContentAsString());accountIds.add(user.get("id").asLong());Session member=new Session((MockHttpSession)logged.getRequest().getSession(false),user);
            perform(member,"GET","/api/medicines/products",null,"MANAGER".equals(role)?200:403);
            perform(member,"POST","/api/medicines/movements/"+m.get("id").asLong()+"/reverse",data("requestKey",key(),"reason","Manager cannot reverse"),403);
            perform(member,"POST","/api/medicines/movements",moveBody(lot,"OWN_USE",1),"MANAGER".equals(role)?200:403);
        }
        mvc.perform(post("/api/medicines/movements").session(owner.session()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(moveBody(lot,"SALE",1)))).andExpect(status().isForbidden());
    }
}
