package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.HarvestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class HarvestServiceImpl implements HarvestService {

    private final HarvestRepository harvestRepository;
    private final BatchStageRepository batchStages;
    private final BatchMovementRepository batchMovements;
    private final PondRepository pondRepository;
    private final FishTypeRepository fishTypeRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public HarvestResponse create(HarvestRequest request) {
        pondRepository.findForFeedingUpdate(request.getPondId()).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Pond not found"));
        if(batchStages.findByPondIdAndCurrentCountGreaterThan(request.getPondId(),0).isPresent())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"This pond has an active batch. Record its harvest in Batches to update fish counts and costing.");
        return createForBatch(request);
    }

    @Override
    public HarvestResponse createForBatch(HarvestRequest request) {

        Pond pond = pondRepository.findById(request.getPondId()).orElseThrow(() -> new RuntimeException("Pond not found"));

        Harvest harvest = new Harvest();

        harvest.setPond(pond);
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setRemarks(request.getRemarks());
        harvest.setDeleted(false);

        harvest.setHarvestNumber(generateHarvestNumber());

        List<HarvestItem> items = new ArrayList<>();

        for (HarvestItemRequest itemRequest : request.getItems()) {

            FishType fishType = fishTypeRepository.findById(itemRequest.getFishTypeId()).orElseThrow(() -> new RuntimeException("Fish type not found"));

            HarvestItem item = new HarvestItem();

            item.setHarvest(harvest);
            item.setFishType(fishType);
            item.setWeightKg(itemRequest.getWeightKg());

            items.add(item);
        }

        harvest.setItems(items);

        Harvest saved = harvestRepository.save(harvest);


/**
 * Update Inventory
 */
        for (HarvestItem item : saved.getItems()) {

            Inventory inventory = inventoryRepository.findByFishType(item.getFishType()).orElseGet(() -> {

                Inventory newInventory = new Inventory();
                newInventory.setFishType(item.getFishType());
                newInventory.setCurrentStockKg(BigDecimal.ZERO);

                return newInventory;
            });

            inventory.setCurrentStockKg(inventory.getCurrentStockKg().add(item.getWeightKg()));

            inventory.setLastUpdated(LocalDateTime.now());

            inventoryRepository.save(inventory);
        }

        return mapToResponse(saved);
    }

    private String generateHarvestNumber() {
        return "H-" + System.currentTimeMillis();
    }

    private HarvestResponse mapToResponse(Harvest harvest) {

        List<HarvestItemResponse> itemResponses = harvest.getItems().stream().map(item -> HarvestItemResponse.builder().fishTypeId(item.getFishType().getId()).fishTypeName(item.getFishType().getName()).weightKg(item.getWeightKg()).build()).toList();

        return HarvestResponse.builder().id(harvest.getId()).harvestNumber(harvest.getHarvestNumber()).pondId(harvest.getPond().getId()).pondName(harvest.getPond().getPondName()).harvestDate(harvest.getHarvestDate()).remarks(harvest.getRemarks()).items(itemResponses).build();
    }

    @Override
    public List<HarvestResponse> getAll() {

        return harvestRepository.findAllWithItems().stream().map(this::mapToResponse).toList();
    }

    @Override
    public HarvestResponse getById(Long id) {

        Harvest harvest = harvestRepository.findByIdWithItems(id).orElseThrow(() -> new RuntimeException("Harvest not found"));

        return mapToResponse(harvest);
    }

    @Override
    public void delete(Long id) {
        if(batchMovements.existsByHarvestId(id))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"A batch harvest cannot be deleted from this page; its count and cost ledger must remain intact.");

        Harvest harvest = harvestRepository.findById(id).orElseThrow(() -> new RuntimeException("Harvest not found"));

        harvest.setDeleted(true);

        harvestRepository.save(harvest);
    }
}