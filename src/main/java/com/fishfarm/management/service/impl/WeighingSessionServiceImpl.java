package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.WeighingSessionRequest;
import com.fishfarm.management.dto.WeighingSessionResponse;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.WeighingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WeighingSessionServiceImpl implements WeighingSessionService {

    private final WeighingSessionRepository repository;
    private final DispatchRepository dispatchRepository;
    private final FishTypeRepository fishTypeRepository;
    private final WeightEntryRepository weightEntryRepository;
    private final PondRepository pondRepository;// <-- naya field add kiya

    @Override
    public WeighingSessionResponse create(WeighingSessionRequest request) {

        if (repository.existsByDispatchIdAndFishTypeId(request.getDispatchId(), request.getFishTypeId())) {
            throw new RuntimeException("Session already exists");
        }

        Dispatch dispatch = dispatchRepository.findById(request.getDispatchId())
                .orElseThrow(() -> new RuntimeException("Dispatch not found"));

        FishType fishType = fishTypeRepository.findById(request.getFishTypeId())
                .orElseThrow(() -> new RuntimeException("Fish type not found"));

        Pond pond = pondRepository.findById(request.getPondId())
                .orElseThrow(() -> new RuntimeException("Pond not found"));

        WeighingSession session = new WeighingSession();
        session.setDispatch(dispatch);
        session.setFishType(fishType);
        session.setSessionLabel(request.getSessionLabel());
        session.setIsClosed(false);
        session.setPond(pond);

        WeighingSession saved = repository.save(session);
        return mapToResponse(saved);
    }

    @Override
    public List<WeighingSessionResponse> getAll() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public WeighingSessionResponse getById(Long id) {
        return mapToResponse(repository.findById(id).orElseThrow(() -> new RuntimeException("Session not found")));
    }

    @Override
    public WeighingSessionResponse closeSession(Long id, BigDecimal ratePerKg, BigDecimal deductionPct) {
        WeighingSession session = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        BigDecimal grossWeight = weightEntryRepository.findBySessionId(id).stream()
                .map(WeightEntry::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal retainFraction = BigDecimal.ONE.subtract(deductionPct.divide(new BigDecimal("100")));
        BigDecimal netWeight = grossWeight.multiply(retainFraction);
        BigDecimal totalAmount = netWeight.multiply(ratePerKg);

        session.setDeductionPct(deductionPct);   // naya field
        session.setRatePerKg(ratePerKg);
        session.setNetWeightKg(netWeight);
        session.setTotalAmount(totalAmount);
        session.setIsClosed(true);

        return mapToResponse(repository.save(session));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private WeighingSessionResponse mapToResponse(WeighingSession session) {
        return WeighingSessionResponse.builder()
                .id(session.getId())
                .dispatchId(session.getDispatch().getId())
                .dispatchNumber(session.getDispatch().getDispatchNumber())
                .fishTypeId(session.getFishType().getId())
                .fishTypeName(session.getFishType().getName())
                .sessionLabel(session.getSessionLabel())
                .isClosed(session.getIsClosed())
                .ratePerKg(session.getRatePerKg())          // <-- naya
                .netWeightKg(session.getNetWeightKg())      // <-- naya
                .totalAmount(session.getTotalAmount())
                .pondId(session.getPond() != null ? session.getPond().getId() : null)
                .pondNumber(session.getPond() != null ? session.getPond().getPondNumber() : null)
                .pondName(session.getPond() != null ? session.getPond().getPondName() : null)// <-- naya
                .build();
    }
}