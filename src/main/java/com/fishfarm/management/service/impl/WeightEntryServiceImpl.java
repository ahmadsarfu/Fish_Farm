package com.fishfarm.management.service.impl;


import com.fishfarm.management.dto.WeighingSessionSummaryResponse;
import com.fishfarm.management.dto.WeightEntryRequest;
import com.fishfarm.management.dto.WeightEntryResponse;
import com.fishfarm.management.entity.WeightEntry;
import com.fishfarm.management.entity.WeighingSession;
import com.fishfarm.management.repository.WeightEntryRepository;
import com.fishfarm.management.repository.WeighingSessionRepository;
import com.fishfarm.management.service.WeightEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeightEntryServiceImpl implements WeightEntryService {

    private final WeightEntryRepository repository;
    private final WeighingSessionRepository sessionRepository;
    private final WeightEntryRepository weightEntryRepository;

    @Override
    public WeightEntryResponse create(WeightEntryRequest request) {

        WeighingSession session = sessionRepository.findById(request.getSessionId()).orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getIsClosed()) {
            throw new RuntimeException("Session already closed");
        }

        Integer basketNo = repository.countBySessionId(request.getSessionId()) + 1;

        WeightEntry entry = new WeightEntry();

        entry.setSession(session);
        entry.setBasketNumber(basketNo);
        entry.setWeightKg(request.getWeightKg());
        entry.setRemarks(request.getRemarks());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setCreatedBy(com.fishfarm.management.security.FarmContext.userId());

        WeightEntry saved = repository.save(entry);

        return mapToResponse(saved);
    }

    @Override
    public WeightEntryResponse getById(Long id) {

        return mapToResponse(repository.findById(id).orElseThrow(() -> new RuntimeException("Weight entry not found")));
    }

    @Override
    public List<WeightEntryResponse> getBySession(Long sessionId) {

        return repository.findBySessionId(sessionId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public void delete(Long id) {

        repository.deleteById(id);
    }

    private WeightEntryResponse mapToResponse(WeightEntry entry) {

        return WeightEntryResponse.builder().id(entry.getId()).sessionId(entry.getSession().getId()).basketNumber(entry.getBasketNumber()).weightKg(entry.getWeightKg()).remarks(entry.getRemarks()).build();
    }

    @Override
    public WeighingSessionSummaryResponse getSummary(Long sessionId) {

        WeighingSession session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException("Session not found"));

        List<WeightEntry> entries =
                weightEntryRepository.findBySessionId(sessionId);

        BigDecimal totalWeight = entries.stream()
                .map(WeightEntry::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WeighingSessionSummaryResponse.builder()
                .sessionId(sessionId)
                .fishTypeId(session.getFishType().getId())
                .fishTypeName(session.getFishType().getName())
                .totalBaskets(entries.size())
                .totalWeightKg(totalWeight)
                .build();
    }
}