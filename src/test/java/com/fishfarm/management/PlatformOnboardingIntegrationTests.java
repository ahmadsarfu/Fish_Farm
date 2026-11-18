package com.fishfarm.management;

import com.fasterxml.jackson.databind.*;
import com.fishfarm.management.entity.AppAccount;
import com.fishfarm.management.repository.AppAccountRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.business-zone=UTC","app.platform-totp-secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"})
@AutoConfigureMockMvc
class PlatformOnboardingIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    final Set<Long> accountIds=new HashSet<>(),farmIds=new HashSet<>();
    final String suffix=UUID.randomUUID().toString();
    final String ip="192.0.2."+new java.util.Random().nextInt(240);
    record Session(MockHttpSession session,JsonNode user){}
    Map<String,Object> data(Object... pairs){Map<String,Object> map=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]);return map;}
    MvcResult perform(Session session,String method,String path,Object body,int expected) throws Exception {
        MockHttpServletRequestBuilder request=switch(method){case "POST"->post(path);case "PUT"->put(path);default->get(path);};
        request.with(r->{r.setRemoteAddr(ip);return r;});if(session!=null)request.session(session.session());
        if(!method.equals("GET"))request.with(csrf());if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(request).andExpect(status().is(expected)).andReturn();
    }
    JsonNode call(Session session,String method,String path,Object body,int expected) throws Exception {return json.readTree(perform(session,method,path,body,expected).getResponse().getContentAsString());}
    String otp() throws Exception {
        Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec("12345678901234567890".getBytes(StandardCharsets.US_ASCII),"HmacSHA1"));
        byte[] hash=mac.doFinal(ByteBuffer.allocate(8).putLong(Instant.now().getEpochSecond()/30).array());int index=hash[hash.length-1]&15;
        return String.format(Locale.ROOT,"%06d",(ByteBuffer.wrap(hash,index,4).getInt()&0x7fffffff)%1000000);
    }
    Session admin() throws Exception {
        AppAccount account=new AppAccount();account.setUsername("admin-"+suffix);account.setDisplayName("Test Platform Owner");account.setPasswordHash(encoder.encode("Platform-password-123"));account.setPlatformAdmin(true);account=accounts.saveAndFlush(account);accountIds.add(account.getId());
        var result=perform(null,"POST","/api/auth/login",data("username",account.getUsername(),"password","Platform-password-123","otp",otp()),200);
        return new Session((MockHttpSession)result.getRequest().getSession(false),json.readTree(result.getResponse().getContentAsString()));
    }
    JsonNode invite(Session admin,String mobile,String email,int ponds,int users) throws Exception {
        var result=call(admin,"POST","/api/platform/customers",data("ownerName","Test Buyer","farmName","Buyer Farm "+suffix,"mobile",mobile,"email",email,"planName","Starter","trialDays",30,"pondLimit",ponds,"userLimit",users),200);
        long id=result.get("customer").get("id").asLong();farmIds.add(id);accountIds.add(jdbc.queryForObject("SELECT account_id FROM customer_invitations WHERE farm_id=?",Long.class,id));return result;
    }
    String token(JsonNode invite){return invite.get("activationPath").asText().split("token=")[1];}
    Session activate(JsonNode invite) throws Exception {
        var result=perform(null,"POST","/api/auth/activate",data("token",token(invite),"password","Customer-password-123"),200);
        return new Session((MockHttpSession)result.getRequest().getSession(false),json.readTree(result.getResponse().getContentAsString()));
    }
    @AfterEach void cleanup(){
        for(long farm:farmIds){
            for(String table:List.of("platform_audit_events","customer_invitations","pond_assignments","farm_audit_events","ponds","farm_members"))jdbc.update("DELETE FROM "+table+" WHERE farm_id=?",farm);
            jdbc.update("DELETE FROM farms WHERE id=?",farm);
        }
        for(long account:accountIds)jdbc.update("DELETE FROM app_accounts WHERE id=?",account);
    }
    String phone(){return "+919"+String.format(Locale.ROOT,"%09d",Math.abs(suffix.hashCode())%1000000000);}
    @Test void adminNeedsMfaAndCannotReadFarmOperations() throws Exception {
        Session admin=admin();assertThat(admin.user().get("role").asText()).isEqualTo("SUPER_ADMIN");assertThat(admin.user().get("farmId").isNull()).isTrue();
        perform(null,"POST","/api/auth/login",data("username",admin.user().get("username").asText(),"password","Platform-password-123"),401);
        perform(admin,"GET","/api/ponds",null,403);perform(null,"GET","/api/platform/customers",null,401);
        mvc.perform(post("/api/platform/customers").session(admin.session()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }
    @Test void inviteActivateAndUniqueContactLoginPreserveTenantBoundaries() throws Exception {
        Session admin=admin();String email="buyer-"+suffix+"@example.test";JsonNode invitation=invite(admin,phone(),email.toUpperCase(Locale.ROOT),2,2);
        String raw=token(invitation);assertThat(jdbc.queryForObject("SELECT token_hash FROM customer_invitations WHERE farm_id=?",String.class,invitation.get("customer").get("id").asLong())).isNotEqualTo(raw);
        call(null,"POST","/api/auth/invitation",data("token",raw),200);
        perform(null,"POST","/api/auth/login",data("username",phone(),"password","Customer-password-123"),401);
        perform(admin,"POST","/api/platform/customers",data("ownerName","Other","farmName","Other","mobile",phone().substring(3),"email","different@example.test","planName","Trial","trialDays",30,"pondLimit",2,"userLimit",2),409);
        Session customer=activate(invitation);assertThat(customer.user().get("subscription").get("accessAllowed").asBoolean()).isTrue();
        perform(customer,"GET","/api/platform/customers",null,403);var requested=call(customer,"POST","/api/auth/farms",data("name","Requested farm"),200);
        long requestedId=requested.get(requested.size()-1).get("id").asLong();farmIds.add(requestedId);
        assertThat(jdbc.queryForObject("SELECT subscription_status FROM farms WHERE id=?",String.class,requestedId)).isEqualTo("SUSPENDED");
        perform(null,"POST","/api/auth/activate",data("token",raw,"password","Another-password-123"),400);
        perform(null,"POST","/api/auth/login",data("username",email,"password","Customer-password-123"),200);
        perform(null,"POST","/api/auth/login",data("username",phone().substring(3),"password","Customer-password-123"),200);
        var listed=call(admin,"GET","/api/platform/customers?search=Buyer",null,200);assertThat(listed.toString()).doesNotContain(raw,"tokenHash","passwordHash");
    }
    @Test void limitsExpiryAndPauseAreEnforcedOnExistingSessions() throws Exception {
        Session admin=admin();var invitation=invite(admin,phone(),"limits-"+suffix+"@example.test",1,1);Session customer=activate(invitation);long id=customer.user().get("farmId").asLong();
        perform(customer,"POST","/api/ponds",data("pondNumber","P1","pondName","First pond"),200);
        perform(customer,"POST","/api/ponds",data("pondNumber","P2","pondName","Over limit"),409);
        perform(customer,"POST","/api/team",data("displayName","Labour","username","labour-"+suffix,"password","Labour-password-123","role","LABOUR","pondIds",List.of()),409);
        call(admin,"PUT","/api/platform/customers/"+id+"/subscription",data("status","SUSPENDED","planName","Starter","pondLimit",1,"userLimit",1),200);
        perform(customer,"GET","/api/ponds",null,403);assertThat(call(customer,"GET","/api/auth/me",null,200).get("subscription").get("accessAllowed").asBoolean()).isFalse();
        call(admin,"PUT","/api/platform/customers/"+id+"/subscription",data("status","ACTIVE","planName","Growth","endsOn",LocalDate.now().plusDays(30).toString(),"pondLimit",3,"userLimit",3),200);
        perform(customer,"GET","/api/ponds",null,200);
        jdbc.update("UPDATE farms SET subscription_ends_on=? WHERE id=?",LocalDate.now().minusDays(1),id);
        perform(customer,"GET","/api/ponds",null,403);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ponds WHERE farm_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void reissuingInvalidatesPreviousLinkAndExpiredLinksCannotActivate() throws Exception {
        Session admin=admin();var invitation=invite(admin,phone(),"reissue-"+suffix+"@example.test",1,1);long id=invitation.get("customer").get("id").asLong();
        var next=call(admin,"POST","/api/platform/customers/"+id+"/activation-link",null,200);
        perform(null,"POST","/api/auth/invitation",data("token",token(invitation)),400);
        jdbc.update("UPDATE customer_invitations SET expires_at=? WHERE farm_id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),id);
        perform(null,"POST","/api/auth/activate",data("token",token(next),"password","Customer-password-123"),400);
    }
    @Test void passwordResetRevokesSessionsAndDoesNotExtendSubscription() throws Exception {
        Session admin=admin();var invitation=invite(admin,phone(),"reset-"+suffix+"@example.test",2,2);Session customer=activate(invitation);long id=customer.user().get("farmId").asLong();
        var before=jdbc.queryForObject("SELECT subscription_ends_on FROM farms WHERE id=?",java.sql.Date.class,id);
        var reset=call(admin,"POST","/api/platform/customers/"+id+"/password-reset",null,200);
        assertThat(reset.get("purpose").asText()).isEqualTo("PASSWORD_RESET");
        perform(null,"POST","/api/auth/activate",data("token",token(reset),"password","Changed-password-123"),200);
        perform(customer,"GET","/api/auth/me",null,401);
        perform(null,"POST","/api/auth/login",data("username",phone(),"password","Customer-password-123"),401);
        perform(null,"POST","/api/auth/login",data("username",phone(),"password","Changed-password-123"),200);
        perform(null,"POST","/api/auth/activate",data("token",token(reset),"password","Another-password-123"),400);
        assertThat(jdbc.queryForObject("SELECT subscription_ends_on FROM farms WHERE id=?",java.sql.Date.class,id)).isEqualTo(before);
    }
    @Test void concurrentActivationConsumesTheLinkOnlyOnce() throws Exception {
        Session admin=admin();var invitation=invite(admin,phone(),"concurrent-"+suffix+"@example.test",2,2);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> action=()->mvc.perform(post("/api/auth/activate").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(data("token",token(invitation),"password","Customer-password-123")))).andReturn().getResponse().getStatus();
            var first=pool.submit(action);var second=pool.submit(action);
            assertThat(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,400);
        } finally {pool.shutdownNow();}
    }

}
