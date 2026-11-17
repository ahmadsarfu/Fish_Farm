package com.fishfarm.management.security;
import com.fishfarm.management.repository.PondAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component @RequiredArgsConstructor
public class FarmAccess {
    private final PondAssignmentRepository assignments;
    public boolean canFeed(Long pondId) {
        return !"LABOUR".equals(FarmContext.role()) || assignments.existsByAccountIdAndPondId(FarmContext.userId(),pondId);
    }
    public void requireFeed(Long pondId) {
        if (!canFeed(pondId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This pond is not assigned to you");
    }
}
