package com.fishfarm.management.service;
import com.fishfarm.management.dto.BatchRequests.*;
import com.fishfarm.management.entity.DailyFeed;
import java.util.*;
public interface BatchService {
    List<Map<String,Object>> list();
    Map<String,Object> details(Long id);
    Map<String,Object> create(Create request);
    Map<String,Object> mortality(Long stageId,Mortality request);
    Map<String,Object> transfer(Long stageId,Transfer request);
    Map<String,Object> expense(Long stageId,Expense request);
    Map<String,Object> harvest(Long stageId,Harvest request);
    Map<String,Object> settlement(Long stageId,com.fishfarm.management.dto.BatchSettlementRequest request);
    void syncFeed(DailyFeed feed);
    void removeFeed(DailyFeed feed);
}
