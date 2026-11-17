package com.fishfarm.management.service;
import java.util.*;
public interface AuditService {
    void record(String action,String type,Long id,String detail);
    List<Map<String,Object>> list();
}
