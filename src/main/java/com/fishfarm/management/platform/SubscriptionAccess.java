package com.fishfarm.management.platform;
import com.fishfarm.management.entity.Farm;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.security.FarmContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
@Component @RequiredArgsConstructor
public class SubscriptionAccess {
    private final FarmRepository farms;
    private final FarmMemberRepository members;
    private final PondRepository ponds;
    private final Clock clock;
    private final jakarta.persistence.EntityManager entityManager;
    private Farm lockedFarm() {
        Farm farm=farms.lockById(FarmContext.farmId()).orElseThrow();
        entityManager.refresh(farm,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);return farm;
    }
    public boolean allowed(Farm farm) {
        return farm.isActive() && !"SUSPENDED".equals(farm.getSubscriptionStatus()) &&
            (farm.getSubscriptionEndsOn()==null || !farm.getSubscriptionEndsOn().isBefore(LocalDate.now(clock)));
    }
    public Map<String,Object> summary(Farm farm) {
        Map<String,Object> value=new LinkedHashMap<>();
        value.put("managed",farm.isManagedSubscription());value.put("status",farm.getSubscriptionStatus());value.put("planName",farm.getPlanName());value.put("endsOn",farm.getSubscriptionEndsOn());
        value.put("pondLimit",farm.getPondLimit());value.put("userLimit",farm.getUserLimit());value.put("accessAllowed",allowed(farm));return value;
    }
    public void requirePondCapacity() {
        Farm farm=lockedFarm();
        if(!allowed(farm)) denied();
        if(farm.getPondLimit()!=null && ponds.countByDeletedFalse()>=farm.getPondLimit()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Your plan's pond limit has been reached. Contact your provider to upgrade.");
    }
    public void requireMemberCapacity() {
        Farm farm=lockedFarm();
        if(!allowed(farm)) denied();
        if(farm.getUserLimit()!=null && members.countByFarmIdAndActiveTrue(farm.getId())>=farm.getUserLimit()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Your plan's active user limit has been reached. Disable an unused member or contact your provider.");
    }
    private void denied() {throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Farm access is paused or expired. Contact your provider.");}
}
