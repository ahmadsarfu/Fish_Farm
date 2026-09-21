package com.fishfarm.management.service.impl;


import com.fishfarm.management.dto.FishTypeRequest;
import com.fishfarm.management.dto.FishTypeResponse;
import com.fishfarm.management.entity.FishType;
import com.fishfarm.management.repository.FishTypeRepository;
import com.fishfarm.management.service.FishTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FishTypeServiceImpl implements FishTypeService {

    private final FishTypeRepository repository;

    @Override
    public FishTypeResponse create(FishTypeRequest request) {

        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new RuntimeException("Fish type already exists");
        }

        FishType fishType = new FishType();
        fishType.setName(request.getName());
        fishType.setActive(true);

        FishType saved = repository.save(fishType);

        return FishTypeResponse.builder().id(saved.getId()).name(saved.getName()).active(saved.getActive()).build();
    }

    @Override
    public List<FishTypeResponse> getAll() {

        return repository.findAll().stream().map(f -> FishTypeResponse.builder().id(f.getId()).name(f.getName()).active(f.getActive()).build()).toList();
    }

    @Override
    public FishTypeResponse update(Long id, FishTypeRequest request) {

        FishType fishType = repository.findById(id).orElseThrow(() -> new RuntimeException("Fish type not found with id: " + id));

        if (repository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {

            throw new IllegalArgumentException("Fish type already exists: " + request.getName());
        }

        fishType.setName(request.getName());
        fishType.setActive(request.getActive());

        FishType saved = repository.save(fishType);

        return mapToResponse(saved);
    }

    @Override
    public FishTypeResponse getById(Long id) {

        FishType fishType = repository.findById(id).orElseThrow(() -> new RuntimeException("Fish type not found with id: " + id));

        return mapToResponse(fishType);
    }

    @Override
    public void delete(Long id) {

        FishType fishType = repository.findById(id).orElseThrow(() -> new RuntimeException("Fish type not found with id: " + id));

        fishType.setActive(false);

        repository.save(fishType);
    }

    private FishTypeResponse mapToResponse(FishType fishType) {

        return FishTypeResponse.builder().id(fishType.getId()).name(fishType.getName()).active(fishType.getActive()).build();
    }
}