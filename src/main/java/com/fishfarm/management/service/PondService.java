package com.fishfarm.management.service;

import com.fishfarm.management.dto.PondRequest;
import com.fishfarm.management.dto.PondResponse;

import java.util.List;

public interface PondService {

    PondResponse create(PondRequest request);

    List<PondResponse> getAll();

    PondResponse getById(Long id);

    PondResponse update(Long id, PondRequest request);

    void delete(Long id);
}