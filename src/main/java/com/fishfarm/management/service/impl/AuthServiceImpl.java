package com.fishfarm.management.service.impl;
import com.fishfarm.management.dto.AuthRequests.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.security.*;
import com.fishfarm.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AuthServiceImpl implements AuthService {
    private final AppAccountRepository accounts;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository members;
    private final PondRepository ponds;
    private final PondAssignmentRepository assignments;
    private final PasswordEncoder encoder;
    private final java.time.Clock clock;
    private final com.fishfarm.management.service.AuditService audit;
    @Value("${app.legacy-setup-token:}") private String setupToken;
    private final com.fishfarm.management.platform.ContactIdentity contacts;
    private final com.fishfarm.management.platform.PlatformMfa mfa;
    private final com.fishfarm.management.platform.SubscriptionAccess subscription;
    @Value("${app.public-registration-enabled:true}") private boolean publicRegistration;
    private String dummyHash;
    @jakarta.annotation.PostConstruct void prepareDummyHash() { dummyHash=encoder.encode(UUID.randomUUID().toString()); }
    private String name(String username) { return username.trim().toLowerCase(Locale.ROOT); }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private void validPassword(String value) {
        if(value.getBytes(StandardCharsets.UTF_8).length>72) throw error(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes");
    }
    public boolean legacyDataWaiting() { return farmRepository.existsByLegacyClaimableTrue(); }
    public boolean legacyClaimAvailable() { return !setupToken.isBlank() && farmRepository.existsByLegacyClaimableTrue(); }
    public FarmPrincipal login(Login request) {
        String identity=name(request.username());
        AppAccount account=accounts.findByUsername(identity).orElseGet(()->accounts.findByEmail(identity).orElse(null));
        if(account==null) {try {account=accounts.findByMobile(contacts.mobile(identity)).orElse(null);} catch(ResponseStatusException ignored) { /* Not a phone: keep generic login error. */ }}
        boolean matches=request.password().getBytes(StandardCharsets.UTF_8).length<=72 && encoder.matches(request.password(),account==null?dummyHash:account.getPasswordHash());
        if(account==null || !account.isActive() || !matches)
            throw error(HttpStatus.UNAUTHORIZED,"Invalid username or password");
        if(account.isPlatformAdmin()) {
            if(!mfa.verify(request.otp()))throw error(HttpStatus.UNAUTHORIZED,"Invalid admin credentials or authenticator code");
            return new FarmPrincipal(account.getId(),account.getUsername(),null,"SUPER_ADMIN",account.getSessionVersion());
        }
        FarmMember member=members.findByAccountIdAndActiveTrueOrderByIdAsc(account.getId()).stream().filter(m->m.getFarm().isActive()).findFirst()
                .orElseThrow(()->error(HttpStatus.FORBIDDEN,"No active farm membership"));
        return principal(member);
    }
    @Transactional public FarmPrincipal register(Register request) {
        if(!publicRegistration && !request.claimExisting())throw error(HttpStatus.FORBIDDEN,"New farms are activated through an invitation from your provider.");
        validPassword(request.password());
        contacts.requireUsernameAvailable(name(request.username()));
        String mobile=contacts.mobile(request.mobile()),email=contacts.email(request.email());
        contacts.requireAvailable(email,mobile);
        Farm farm;
        if(request.claimExisting()) {
            if(setupToken.isBlank() || request.setupToken()==null || !MessageDigest.isEqual(setupToken.getBytes(StandardCharsets.UTF_8),request.setupToken().getBytes(StandardCharsets.UTF_8)))
                throw error(HttpStatus.FORBIDDEN,"Invalid existing-farm setup code");
            farm=farmRepository.lockById(1L).orElseThrow(()->error(HttpStatus.CONFLICT,"Existing farm is unavailable"));
            if(!farm.isLegacyClaimable() || members.existsByFarmId(farm.getId())) throw error(HttpStatus.CONFLICT,"Existing farm has already been claimed");
            farm.setLegacyClaimable(false);
            if(request.farmName()!=null && !request.farmName().isBlank()) farm.setName(request.farmName().trim());
        } else {
            if(request.farmName()==null || request.farmName().isBlank()) throw error(HttpStatus.BAD_REQUEST,"Farm name is required");
            farm=new Farm(); farm.setName(request.farmName().trim()); farm=farmRepository.saveAndFlush(farm);
        }
        AppAccount account=new AppAccount();account.setUsername(name(request.username()));account.setDisplayName(request.displayName().trim());account.setPasswordHash(encoder.encode(request.password()));
        account.setMobile(mobile);account.setEmail(email);
        account=accounts.saveAndFlush(account);
        FarmMember member=new FarmMember();member.setAccount(account);member.setFarm(farm);member.setRole("OWNER");
        return principal(members.saveAndFlush(member));
    }
    public FarmPrincipal selectFarm(Long accountId,Long farmId) {
        FarmMember member=members.findByAccountIdAndFarmIdAndActiveTrue(accountId,farmId).filter(m->m.getFarm().isActive())
                .orElseThrow(()->error(HttpStatus.FORBIDDEN,"Farm access denied"));
        return principal(member);
    }
    private FarmPrincipal principal(FarmMember m) { return new FarmPrincipal(m.getAccount().getId(),m.getAccount().getUsername(),m.getFarm().getId(),m.getRole(),m.getAccount().getSessionVersion()); }
    public List<Map<String,Object>> farms(Long accountId) {
        return members.findByAccountIdAndActiveTrueOrderByIdAsc(accountId).stream().filter(m->m.getFarm().isActive())
                .map(m->Map.<String,Object>of("id",m.getFarm().getId(),"name",m.getFarm().getName(),"role",m.getRole())).toList();
    }
    public Map<String,Object> me(FarmPrincipal principal) {
        AppAccount account=accounts.findById(principal.userId()).orElseThrow();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",account.getId());result.put("username",account.getUsername());result.put("displayName",account.getDisplayName());result.put("farmId",principal.farmId());result.put("role",principal.role());
        result.put("farms",account.isPlatformAdmin()?List.of():farms(account.getId()));result.put("timeZone",clock.getZone().getId());result.put("businessDate",java.time.LocalDate.now(clock).toString());
        result.put("mobile",account.getMobile());result.put("email",account.getEmail());
        if(principal.farmId()!=null)result.put("subscription",subscription.summary(farmRepository.findById(principal.farmId()).orElseThrow()));
        return result;
    }
    @Transactional public void createFarm(Long accountId,String name) {
        boolean managed=members.findByAccountIdAndActiveTrueOrderByIdAsc(accountId).stream().anyMatch(m->m.getFarm().isManagedSubscription());
        Farm farm=new Farm();farm.setName(name.trim());
        if(managed) {farm.setManagedSubscription(true);farm.setPlanName("Awaiting activation");farm.setSubscriptionStatus("SUSPENDED");farm.setPondLimit(1);farm.setUserLimit(1);}
        farm=farmRepository.saveAndFlush(farm);
        FarmMember member=new FarmMember();member.setFarm(farm);member.setAccount(accounts.findById(accountId).orElseThrow());member.setRole("OWNER");members.save(member);
    }
    public List<Map<String,Object>> team() {
        List<PondAssignment> all=assignments.findAll();
        return members.findByFarmIdOrderByIdAsc(FarmContext.farmId()).stream().map(m->{
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",m.getId());row.put("accountId",m.getAccount().getId());row.put("username",m.getAccount().getUsername());row.put("displayName",m.getAccount().getDisplayName());row.put("role",m.getRole());row.put("active",m.isActive());
            row.put("pondIds",all.stream().filter(a->a.getAccountId().equals(m.getAccount().getId())).map(a->a.getPond().getId()).toList());return row;
        }).toList();
    }
    @Transactional public void createMember(NewMember request) {
        subscription.requireMemberCapacity();
        validPassword(request.password());
        contacts.requireUsernameAvailable(name(request.username()));
        List<Pond> selected=selectedPonds(request.pondIds());
        AppAccount account=new AppAccount();account.setUsername(name(request.username()));account.setDisplayName(request.displayName().trim());account.setPasswordHash(encoder.encode(request.password()));account=accounts.saveAndFlush(account);
        FarmMember member=new FarmMember();member.setAccount(account);member.setFarm(farmRepository.findById(FarmContext.farmId()).orElseThrow());member.setRole(request.role());members.saveAndFlush(member);
        assign(account.getId(),selected);
        audit.record("ADD_MEMBER","ACCOUNT",account.getId(),"Added "+request.role()+" to this farm");
    }
    @Transactional public void updateMember(Long id,UpdateMember request) {
        FarmMember member=members.findById(id).filter(m->m.getFarm().getId().equals(FarmContext.farmId())).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Member not found"));
        if("OWNER".equals(member.getRole())) throw error(HttpStatus.BAD_REQUEST,"Owner access cannot be changed here");
        if(!member.isActive() && request.active())subscription.requireMemberCapacity();
        List<Pond> selected=selectedPonds(request.pondIds());
        member.setActive(request.active());member.setRole(request.role());assignments.deleteByAccountId(member.getAccount().getId());assignments.flush();assign(member.getAccount().getId(),selected);
        audit.record("UPDATE_ACCESS","ACCOUNT",member.getAccount().getId(),request.role()+"; active="+request.active());
    }
    private List<Pond> selectedPonds(List<Long> ids) {
        return ids.stream().distinct().map(id->ponds.findById(id).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"A selected pond does not belong to this farm"))).toList();
    }
    private void assign(Long accountId,List<Pond> selected) {
        for(Pond pond:selected) {PondAssignment a=new PondAssignment();a.setAccountId(accountId);a.setPond(pond);assignments.save(a);}
    }
    @Transactional public void changePassword(Long accountId,ChangePassword request) {
        validPassword(request.newPassword());AppAccount account=accounts.findById(accountId).orElseThrow();
        if(!encoder.matches(request.currentPassword(),account.getPasswordHash())) throw error(HttpStatus.BAD_REQUEST,"Current password is incorrect");
        account.setPasswordHash(encoder.encode(request.newPassword()));account.setSessionVersion(account.getSessionVersion()+1);
    }
}
