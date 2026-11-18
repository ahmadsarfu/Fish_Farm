package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.PlatformRequests.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.security.FarmPrincipal;
import com.fishfarm.management.service.PlatformCustomerService;
import com.fishfarm.management.platform.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PlatformCustomerServiceImpl implements PlatformCustomerService {
    private final FarmRepository farms;
    private final AppAccountRepository accounts;
    private final FarmMemberRepository members;
    private final CustomerInvitationRepository invitations;
    private final PlatformAuditRepository audit;
    private final ContactIdentity contacts;
    private final SubscriptionAccess subscriptions;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecureRandom random=new SecureRandom();
    private PageRequest page(int page,int size){return PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)));}
    public Map<String,Object> customers(String search,int page,int size) {
        var result=farms.findByNameContainingIgnoreCaseOrderByIdDesc(search.trim(),page(page,size));
        var ids=result.stream().map(Farm::getId).toList();
        var memberRows=ids.isEmpty()?Map.<Long,List<FarmMember>>of():members.findByFarmIdInOrderByIdAsc(ids).stream().collect(java.util.stream.Collectors.groupingBy(member->member.getFarm().getId()));
        var inviteRows=ids.isEmpty()?Map.<Long,CustomerInvitation>of():invitations.findByFarmIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(invitation->invitation.getFarm().getId(),invitation->invitation));
        return Map.of("items",result.stream().map(farm->row(farm,memberRows.getOrDefault(farm.getId(),List.of()),Optional.ofNullable(inviteRows.get(farm.getId())))).toList(),"total",result.getTotalElements(),"page",result.getNumber(),"pages",result.getTotalPages());
    }
    private Map<String,Object> row(Farm farm) {
        return row(farm,members.findByFarmIdOrderByIdAsc(farm.getId()),invitations.findByFarmId(farm.getId()));
    }
    private Map<String,Object> row(Farm farm,List<FarmMember> farmMembers,Optional<CustomerInvitation> invite) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("id",farm.getId());row.put("farmName",farm.getName());row.put("subscription",subscriptions.summary(farm));
        var owner=farmMembers.stream().filter(member->"OWNER".equals(member.getRole())).findFirst();
        row.put("ownerName",owner.map(m->m.getAccount().getDisplayName()).orElse("Existing farm"));
        row.put("mobile",owner.map(m->m.getAccount().getMobile()).orElse(null));row.put("email",owner.map(m->m.getAccount().getEmail()).orElse(null));
        row.put("onboarding",invite.map(i->i.getAcceptedAt()!=null?"ACTIVATED":i.getExpiresAt().isBefore(clock.instant())?"LINK_EXPIRED":"INVITED").orElse(farm.isManagedSubscription() ? ("Awaiting activation".equals(farm.getPlanName()) ? "AWAITING_PLAN" : "EXISTING_ACCOUNT") : "EXISTING"));
        row.put("activeUsers",farmMembers.stream().filter(FarmMember::isActive).count());return row;
    }
    @Transactional public Map<String,Object> invite(Invite request,Long actorId) {
        String email=contacts.email(request.email()),mobile=contacts.mobile(request.mobile());contacts.requireAvailable(email,mobile);
        Farm farm=new Farm();farm.setName(request.farmName().trim());farm.setPlanName(request.planName().trim());farm.setSubscriptionStatus("TRIAL");farm.setManagedSubscription(true);farm.setPondLimit(request.pondLimit());farm.setUserLimit(request.userLimit());farm=farms.saveAndFlush(farm);
        AppAccount account=new AppAccount();account.setUsername("customer-"+UUID.randomUUID());account.setDisplayName(request.ownerName().trim());account.setMobile(mobile);account.setEmail(email);account.setActive(false);account.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));account=accounts.saveAndFlush(account);
        FarmMember member=new FarmMember();member.setAccount(account);member.setFarm(farm);member.setRole("OWNER");member.setActive(false);members.saveAndFlush(member);
        CustomerInvitation invitation=new CustomerInvitation();invitation.setFarm(farm);invitation.setAccount(account);invitation.setTrialDays(request.trialDays());
        String token=rotate(invitation);invitations.saveAndFlush(invitation);record(actorId,farm.getId(),"CUSTOMER_INVITED");return link(farm,invitation,token);
    }
    private String rotate(CustomerInvitation invitation) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        invitation.setTokenConsumedAt(null);invitation.setTokenHash(hash(token));invitation.setExpiresAt(clock.instant().plus(Duration.ofDays(7)));return token;
    }
    private Map<String,Object> link(Farm farm,CustomerInvitation invitation,String token) {
        return Map.of("customer",row(farm),"activationPath","/activate#token="+token,"expiresAt",invitation.getExpiresAt(),"purpose",invitation.getPurpose());
    }
    @Transactional public Map<String,Object> reissue(Long farmId,Long actorId) {
        Farm farm=farms.lockById(farmId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Customer not found"));
        var invitation=invitations.lockByFarmId(farmId).orElseThrow(()->error(HttpStatus.CONFLICT,"This farm has no pending invitation"));
        if(invitation.getAcceptedAt()!=null)throw error(HttpStatus.CONFLICT,"This customer has already activated their account");
        String token=rotate(invitation);invitations.flush();record(actorId,farmId,"ACTIVATION_LINK_REISSUED");return link(farm,invitation,token);
    }
    @Transactional public Map<String,Object> passwordReset(Long farmId,Long actorId) {
        Farm farm=farms.lockById(farmId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Customer not found"));
        var invitation=invitations.lockByFarmId(farmId).orElseThrow(()->error(HttpStatus.CONFLICT,"This farm uses an existing account. Use its account recovery procedure."));
        if(invitation.getAcceptedAt()==null)throw error(HttpStatus.CONFLICT,"This customer must activate their account first");
        invitation.setPurpose("PASSWORD_RESET");String token=rotate(invitation);invitation.setExpiresAt(clock.instant().plus(Duration.ofHours(1)));
        invitations.flush();record(actorId,farmId,"PASSWORD_RESET_LINK_ISSUED");return link(farm,invitation,token);
    }
    @Transactional public Map<String,Object> update(Long farmId,Subscription request,Long actorId) {
        Farm farm=farms.lockById(farmId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Customer not found"));
        if(!"SUSPENDED".equals(request.status()) && request.endsOn()!=null && request.endsOn().isBefore(LocalDate.now(clock)))throw error(HttpStatus.BAD_REQUEST,"Choose today or a future expiry date");
        if(members.countByFarmIdAndActiveTrue(farmId)>request.userLimit())throw error(HttpStatus.CONFLICT,"User limit cannot be lower than current active users");
        // Domain counts are deliberately not read in the platform tenant. The tenant guard prevents new ponds over the chosen cap.
        farm.setManagedSubscription(true);farm.setPlanName(request.planName().trim());farm.setSubscriptionStatus(request.status());farm.setSubscriptionEndsOn(request.endsOn());farm.setPondLimit(request.pondLimit());farm.setUserLimit(request.userLimit());
        farms.flush();record(actorId,farmId,"SUBSCRIPTION_UPDATED");return row(farm);
    }
    public Map<String,Object> invitation(String token) {
        var invitation=valid(invitations.findByTokenHash(hash(token)).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"This activation link is invalid or has expired. Ask your provider for a new link.")));
        return Map.of("ownerName",invitation.getAccount().getDisplayName(),"farmName",invitation.getFarm().getName(),"planName",invitation.getFarm().getPlanName(),"trialDays",invitation.getTrialDays(),"purpose",invitation.getPurpose());
    }
    @Transactional public FarmPrincipal activate(Activate request) {
        var farmId=invitations.findFarmIdByTokenHash(hash(request.token())).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"This activation link is invalid or has expired. Ask your provider for a new link."));
        farms.lockById(farmId).orElseThrow();
        var invitation=valid(invitations.lockByTokenHash(hash(request.token())).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Activation link has changed. Ask your provider for the latest link.")));
        boolean reset="PASSWORD_RESET".equals(invitation.getPurpose());
        if(!reset && !subscriptions.allowed(invitation.getFarm())) throw error(HttpStatus.FORBIDDEN,"This farm is paused. Contact your provider.");
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72)throw error(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes");
        var account=invitation.getAccount();
        if(reset && !account.isActive())throw error(HttpStatus.FORBIDDEN,"This account is disabled. Contact your provider.");
        account.setPasswordHash(encoder.encode(request.password()));
        if(reset)account.setSessionVersion(account.getSessionVersion()+1);else account.setActive(true);
        var member=members.findByFarmIdOrderByIdAsc(invitation.getFarm().getId()).stream().filter(m->m.getAccount().getId().equals(account.getId())).findFirst().orElseThrow();if(reset && !member.isActive())throw error(HttpStatus.FORBIDDEN,"This farm membership is disabled. Contact your provider.");
        if(!reset)member.setActive(true);
        invitation.setTokenConsumedAt(clock.instant());if(!reset)invitation.setAcceptedAt(clock.instant());if(!reset && "TRIAL".equals(invitation.getFarm().getSubscriptionStatus()) && invitation.getFarm().getSubscriptionEndsOn()==null)invitation.getFarm().setSubscriptionEndsOn(LocalDate.now(clock).plusDays(invitation.getTrialDays()));
        accounts.flush();members.flush();invitations.flush();record(account.getId(),member.getFarm().getId(),reset?"CUSTOMER_PASSWORD_RESET":"CUSTOMER_ACTIVATED");
        return new FarmPrincipal(account.getId(),account.getUsername(),member.getFarm().getId(),"OWNER",account.getSessionVersion());
    }
    private CustomerInvitation valid(CustomerInvitation invitation) {
        if(invitation.getTokenConsumedAt()!=null || ("ACTIVATION".equals(invitation.getPurpose()) && invitation.getAcceptedAt()!=null) || !invitation.getExpiresAt().isAfter(clock.instant()))throw error(HttpStatus.BAD_REQUEST,"This activation link is used or expired. Log in if already activated, or ask your provider for a new link.");
        return invitation;
    }
    private String hash(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
    }
    private void record(Long actor,Long farm,String action) {var event=new PlatformAuditEvent();event.setActorId(actor);event.setFarmId(farm);event.setAction(action);event.setOccurredAt(clock.instant());audit.save(event);}
    public Object activity(int page,int size){return audit.findAllByOrderByIdDesc(page(page,size)).map(event->Map.of("id",event.getId(),"actorId",event.getActorId(),"farmId",event.getFarmId(),"action",event.getAction(),"time",event.getOccurredAt()));}
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
