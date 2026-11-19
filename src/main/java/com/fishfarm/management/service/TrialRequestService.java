package com.fishfarm.management.service;
import com.fishfarm.management.dto.TrialRequests.*;
import java.util.Map;
public interface TrialRequestService {
    void submit(Submit request,String remoteAddress);
    Object list(String status,int page,int size);
    Map<String,Object> approve(Long id,Approve request,Long actor);
    void reject(Long id,Long actor);
}
