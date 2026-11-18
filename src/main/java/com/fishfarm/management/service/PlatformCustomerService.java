package com.fishfarm.management.service;
import com.fishfarm.management.dto.PlatformRequests.*;
import com.fishfarm.management.security.FarmPrincipal;
import java.util.Map;
public interface PlatformCustomerService {
    Map<String,Object> customers(String search,int page,int size);
    Map<String,Object> invite(Invite request,Long actorId);
    Map<String,Object> reissue(Long farmId,Long actorId);
    Map<String,Object> passwordReset(Long farmId,Long actorId);
    Map<String,Object> update(Long farmId,Subscription request,Long actorId);
    Map<String,Object> invitation(String token);
    FarmPrincipal activate(Activate request);
    Object activity(int page,int size);
}
