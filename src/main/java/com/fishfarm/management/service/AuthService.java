package com.fishfarm.management.service;
import com.fishfarm.management.dto.AuthRequests.*;
import com.fishfarm.management.security.FarmPrincipal;
import java.util.*;
public interface AuthService {
    FarmPrincipal login(Login request);
    FarmPrincipal register(Register request);
    FarmPrincipal selectFarm(Long accountId,Long farmId);
    Map<String,Object> me(FarmPrincipal principal);
    List<Map<String,Object>> farms(Long accountId);
    void createFarm(Long accountId,String name);
    boolean legacyClaimAvailable();
    boolean legacyDataWaiting();
    List<Map<String,Object>> team();
    void createMember(NewMember request);
    void updateMember(Long memberId,UpdateMember request);
    void changePassword(Long accountId,ChangePassword request);
}
