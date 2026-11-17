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
        AppAccount account=accounts.findByUsername(name(request.username())).orElse(null);
        boolean matches=request.password().getBytes(StandardCharsets.UTF_8).length<=72 && encoder.matches(request.password(),account==null?dummyHash:account.getPasswordHash());
        if(account==null || !account.isActive() || !matches)
            throw error(HttpStatus.UNAUTHORIZED,"Invalid username or password");
        FarmMember member=members.findByAccountIdAndActiveTrueOrderByIdAsc(account.getId()).stream().filter(m->m.getFarm().isActive()).findFirst()
                .orElseThrow(()->error(HttpStatus.FORBIDDEN,"No active farm membership"));
        return principal(member);
    }
    @Transactional public FarmPrincipal register(Register request) {
        validPassword(request.password());
        if(accounts.existsByUsername(name(request.username()))) throw error(HttpStatus.CONFLICT,"This username is unavailable");
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
        return Map.of("id",account.getId(),"username",account.getUsername(),"displayName",account.getDisplayName(),"farmId",principal.farmId(),"role",principal.role(),"farms",farms(account.getId()),"timeZone",clock.getZone().getId(),"businessDate",java.time.LocalDate.now(clock).toString());
    }
    @Transactional public void createFarm(Long accountId,String name) {
        Farm farm=new Farm();farm.setName(name.trim());farm=farmRepository.saveAndFlush(farm);
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
        validPassword(request.password());
        if(accounts.existsByUsername(name(request.username()))) throw error(HttpStatus.CONFLICT,"This username is unavailable");
        List<Pond> selected=selectedPonds(request.pondIds());
        AppAccount account=new AppAccount();account.setUsername(name(request.username()));account.setDisplayName(request.displayName().trim());account.setPasswordHash(encoder.encode(request.password()));account=accounts.saveAndFlush(account);
        FarmMember member=new FarmMember();member.setAccount(account);member.setFarm(farmRepository.findById(FarmContext.farmId()).orElseThrow());member.setRole(request.role());members.saveAndFlush(member);
        assign(account.getId(),selected);
        audit.record("ADD_MEMBER","ACCOUNT",account.getId(),"Added "+request.role()+" to this farm");
    }
    @Transactional public void updateMember(Long id,UpdateMember request) {
        FarmMember member=members.findById(id).filter(m->m.getFarm().getId().equals(FarmContext.farmId())).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Member not found"));
        if("OWNER".equals(member.getRole())) throw error(HttpStatus.BAD_REQUEST,"Owner access cannot be changed here");
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
