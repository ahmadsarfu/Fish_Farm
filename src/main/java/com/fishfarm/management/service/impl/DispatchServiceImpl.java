package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.DispatchRequest;
import com.fishfarm.management.dto.DispatchResponse;
import com.fishfarm.management.entity.Dispatch;
import com.fishfarm.management.entity.WeighingSession;
import com.fishfarm.management.enums.DispatchStatus;
import com.fishfarm.management.repository.DispatchRepository;
import com.fishfarm.management.repository.WeighingSessionRepository;
import com.fishfarm.management.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    private final DispatchRepository repository;

    private final WeighingSessionRepository weighingSessionRepository;

    @Override
    public DispatchResponse create(DispatchRequest request) {

        Dispatch dispatch = new Dispatch();

        dispatch.setDispatchNumber(generateDispatchNumber());

        dispatch.setVehicleNumber(request.getVehicleNumber());

        dispatch.setDriverName(request.getDriverName());

        dispatch.setDispatchDate(request.getDispatchDate());

        dispatch.setCreatedBy(1L);

        dispatch.setDriverPhone(request.getDriverPhone());

        dispatch.setStatus(DispatchStatus.CREATED);

        Dispatch saved = repository.save(dispatch);

        return mapToResponse(saved);
    }

    private String generateDispatchNumber() {
        return "D-" + System.currentTimeMillis();
    }

    @Override
    public List<DispatchResponse> getAll() {

        return repository.findByDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public DispatchResponse getById(Long id) {

        Dispatch dispatch = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Dispatch not found"));

        return mapToResponse(dispatch);
    }

    @Override
    public void delete(Long id) {

        Dispatch dispatch = repository.findById(id).orElseThrow(() -> new RuntimeException("Dispatch not found"));

        dispatch.setDeleted(true);

        repository.save(dispatch);
    }

    @Override
    public DispatchResponse markAsDispatched(Long id) {

        Dispatch dispatch = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Dispatch not found"));

        List<WeighingSession> sessions =
                weighingSessionRepository.findByDispatchId(id);

        if (sessions.isEmpty()) {
            throw new RuntimeException(
                    "Cannot complete dispatch. No fish weighing found."
            );
        }

        boolean hasPendingSession = sessions.stream()
                .anyMatch(session ->
                        !Boolean.TRUE.equals(session.getIsClosed())
                );

        if (hasPendingSession) {
            throw new RuntimeException(
                    "Cannot complete dispatch. Complete all fish weighing first."
            );
        }

        dispatch.setStatus(DispatchStatus.DISPATCHED);

        Dispatch saved = repository.save(dispatch);

        return mapToResponse(saved);
    }


    private DispatchResponse mapToResponse(Dispatch dispatch) {

        return DispatchResponse.builder().id(dispatch.getId()).dispatchNumber(dispatch.getDispatchNumber()).driverPhone(dispatch.getDriverPhone()).vehicleNumber(dispatch.getVehicleNumber()).driverName(dispatch.getDriverName()).dispatchDate(dispatch.getDispatchDate()).status(dispatch.getStatus()).build();
    }
}