package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.SalesHistoryItemResponse;
import com.fishfarm.management.dto.SalesHistoryResponse;
import com.fishfarm.management.entity.Dispatch;
import com.fishfarm.management.entity.WeighingSession;
import com.fishfarm.management.enums.DispatchStatus;
import com.fishfarm.management.repository.DispatchRepository;
import com.fishfarm.management.repository.WeighingSessionRepository;
import com.fishfarm.management.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesServiceImpl implements SalesService {

    private final DispatchRepository dispatchRepository;
    private final WeighingSessionRepository weighingSessionRepository;

    @Override
    public SalesHistoryResponse getHistory() {

        List<Dispatch> dispatched =
                dispatchRepository.findByStatusAndDeletedFalse(
                        DispatchStatus.DISPATCHED
                );

        List<SalesHistoryItemResponse> history = new ArrayList<>();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Dispatch dispatch : dispatched) {

            List<WeighingSession> sessions =
                    weighingSessionRepository.findByDispatchId(
                            dispatch.getId()
                    );

            // Only completed fish weighing
            List<WeighingSession> completedSessions =
                    sessions.stream()
                            .filter(s ->
                                    Boolean.TRUE.equals(s.getIsClosed())
                            )
                            .toList();

            BigDecimal dispatchWeight =
                    completedSessions.stream()
                            .map(s ->
                                    s.getNetWeightKg() != null
                                            ? s.getNetWeightKg()
                                            : BigDecimal.ZERO
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            BigDecimal dispatchAmount =
                    completedSessions.stream()
                            .map(s ->
                                    s.getTotalAmount() != null
                                            ? s.getTotalAmount()
                                            : BigDecimal.ZERO
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            history.add(
                    SalesHistoryItemResponse.builder()
                            .dispatchId(dispatch.getId())
                            .dispatchNumber(dispatch.getDispatchNumber())
                            .vehicleNumber(dispatch.getVehicleNumber())
                            .driverName(dispatch.getDriverName())
                            .driverPhone(dispatch.getDriverPhone())
                            .dispatchDate(dispatch.getDispatchDate())
                            .totalWeightKg(dispatchWeight)
                            .totalAmount(dispatchAmount)
                            .build()
            );

            totalWeight = totalWeight.add(dispatchWeight);
            totalSales = totalSales.add(dispatchAmount);
        }

        // latest dispatch first
        history.sort(
                (a, b) ->
                        b.getDispatchDate()
                                .compareTo(a.getDispatchDate())
        );

        return SalesHistoryResponse.builder()
                .totalSales(totalSales)
                .totalWeightKg(totalWeight)
                .totalDispatches((long) history.size())
                .dispatches(history)
                .build();
    }
}