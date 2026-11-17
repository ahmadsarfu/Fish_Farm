package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.FishStockEntryRequest;
import com.fishfarm.management.dto.FishStockEntryResponse;
import com.fishfarm.management.entity.FishStockEntry;
import com.fishfarm.management.entity.FishType;
import com.fishfarm.management.entity.Pond;
import com.fishfarm.management.repository.FishStockEntryRepository;
import com.fishfarm.management.repository.FishTypeRepository;
import com.fishfarm.management.repository.PondRepository;
import com.fishfarm.management.service.FishStockEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FishStockEntryServiceImpl
        implements FishStockEntryService {

    private final FishStockEntryRepository repository;
    private final FishTypeRepository fishTypeRepository;
    private final PondRepository pondRepository;

    @Override
    public FishStockEntryResponse create(
            FishStockEntryRequest request
    ) {

        FishType fishType = fishTypeRepository
                .findById(request.getFishTypeId())
                .orElseThrow(() ->
                        new RuntimeException("Fish type not found")
                );

        Pond pond = pondRepository
                .findById(request.getPondId())
                .orElseThrow(() ->
                        new RuntimeException("Pond not found")
                );

        validateSize(request);

        FishStockEntry entry = new FishStockEntry();

        entry.setFishType(fishType);
        entry.setPond(pond);
        entry.setStockDate(request.getStockDate());

        entry.setSizeFromGram(request.getSizeFromGram());
        entry.setSizeToGram(request.getSizeToGram());

        entry.setQuantity(request.getQuantity());
        entry.setPurchaseAmount(request.getPurchaseAmount());

        entry.setSupplierName(request.getSupplierName());
        entry.setRemarks(request.getRemarks());

        entry.setCreatedBy(1L);
        entry.setDeleted(false);

        return mapToResponse(repository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FishStockEntryResponse> getAll() {

        return repository
                .findByDeletedFalseOrderByStockDateDescIdDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FishStockEntryResponse getById(Long id) {

        FishStockEntry entry = repository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Stock entry not found")
                );

        return mapToResponse(entry);
    }

    @Override
    public FishStockEntryResponse update(
            Long id,
            FishStockEntryRequest request
    ) {

        FishStockEntry entry = repository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Stock entry not found")
                );

        FishType fishType = fishTypeRepository
                .findById(request.getFishTypeId())
                .orElseThrow(() ->
                        new RuntimeException("Fish type not found")
                );

        Pond pond = pondRepository
                .findById(request.getPondId())
                .orElseThrow(() ->
                        new RuntimeException("Pond not found")
                );

        validateSize(request);

        entry.setFishType(fishType);
        entry.setPond(pond);
        entry.setStockDate(request.getStockDate());

        entry.setSizeFromGram(request.getSizeFromGram());
        entry.setSizeToGram(request.getSizeToGram());

        entry.setQuantity(request.getQuantity());
        entry.setPurchaseAmount(request.getPurchaseAmount());

        entry.setSupplierName(request.getSupplierName());
        entry.setRemarks(request.getRemarks());

        entry.setUpdatedBy(1L);

        return mapToResponse(repository.save(entry));
    }

    @Override
    public void delete(Long id) {

        FishStockEntry entry = repository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Stock entry not found")
                );

        entry.setDeleted(true);
        entry.setUpdatedBy(1L);

        repository.save(entry);
    }

    private void validateSize(FishStockEntryRequest request) {

        if (request.getSizeFromGram() != null
                && request.getSizeToGram() != null
                && request.getSizeFromGram()
                .compareTo(request.getSizeToGram()) > 0) {

            throw new RuntimeException(
                    "Starting size from cannot be greater than size to"
            );
        }
    }

    private FishStockEntryResponse mapToResponse(
            FishStockEntry entry
    ) {

        return FishStockEntryResponse.builder()
                .id(entry.getId())

                .fishTypeId(entry.getFishType().getId())
                .fishTypeName(entry.getFishType().getName())

                .pondId(entry.getPond().getId())
                .pondNumber(entry.getPond().getPondNumber())
                .pondName(entry.getPond().getPondName())

                .stockDate(entry.getStockDate())

                .sizeFromGram(entry.getSizeFromGram())
                .sizeToGram(entry.getSizeToGram())

                .quantity(entry.getQuantity())

                .purchaseAmount(entry.getPurchaseAmount())

                .supplierName(entry.getSupplierName())
                .remarks(entry.getRemarks())

                .createdAt(entry.getCreatedAt())

                .build();
    }
}