package com.fishfarm.management.service.impl;
import com.fishfarm.management.dto.TrialRequests.*;
import com.fishfarm.management.dto.PlatformRequests.Invite;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.platform.ContactIdentity;
import com.fishfarm.management.security.LoginThrottle;
import com.fishfarm.management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TrialRequestServiceImpl implements TrialRequestService {
    private final TrialRequestRepository requests;
    private final PlatformCustomerService customers;
    private final ContactIdentity contacts;
    private final LoginThrottle throttle;
    private final Clock clock;
    @Transactional public void submit(Submit request,String remoteAddress) {
        String key="trial:"+remoteAddress;throttle.check(key);throttle.failure(key);
        requests.submit(request.ownerName().trim(),request.farmName().trim(),contacts.mobile(request.mobile()),contacts.email(request.email()),clock.instant());
    }
    public Object list(String status,int page,int size) {
        if(!Set.of("PENDING","APPROVED","REJECTED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid request status");
        var result=requests.findByStatusOrderByIdDesc(status,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100))));
        return Map.of("items",result.getContent(),"total",result.getTotalElements(),"pages",result.getTotalPages());
    }
    private TrialRequest pending(Long id) {
        var row=requests.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trial request not found"));
        if(!"PENDING".equals(row.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been reviewed");
        return row;
    }
    @Transactional public Map<String,Object> approve(Long id,Approve request,Long actor) {
        var row=pending(id);
        var result=customers.invite(new Invite(row.getOwnerName(),row.getFarmName(),row.getMobile(),row.getEmail(),request.planName(),request.trialDays(),request.pondLimit(),request.userLimit()),actor);
        row.setFarmId(((Number)((Map<?,?>)result.get("customer")).get("id")).longValue());
        review(row,"APPROVED",actor);return result;
    }
    @Transactional public void reject(Long id,Long actor) {review(pending(id),"REJECTED",actor);}
    private void review(TrialRequest row,String status,Long actor) {
        row.setStatus(status);row.setReviewedAt(clock.instant());row.setReviewedBy(actor);
    }
}
