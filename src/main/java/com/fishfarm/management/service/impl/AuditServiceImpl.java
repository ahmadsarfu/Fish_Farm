package com.fishfarm.management.service.impl;
import com.fishfarm.management.entity.AuditEvent;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AuditServiceImpl implements AuditService {
    private final AuditEventRepository events;
    private final AppAccountRepository accounts;
    @Transactional public void record(String action,String type,Long id,String detail) {
        AuditEvent e=new AuditEvent();e.setAction(action);e.setEntityType(type);e.setEntityId(id);e.setDetail(detail);events.save(e);
    }
    public List<Map<String,Object>> list() {
        return events.findTop100ByOrderByIdDesc().stream().map(e->{
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",e.getId());row.put("action",e.getAction());row.put("detail",e.getDetail());row.put("time",e.getRecordedAt());
            row.put("actor",e.getRecordedBy()==null?"Legacy record":accounts.findById(e.getRecordedBy()).map(a->a.getDisplayName()).orElse("Legacy record"));return row;
        }).toList();
    }
}
