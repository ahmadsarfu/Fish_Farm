package com.fishfarm.management.service;

import com.fishfarm.management.dto.DispatchRequest;
import com.fishfarm.management.dto.DispatchResponse;

import java.util.List;

public interface DispatchService {

    DispatchResponse create(DispatchRequest request);

    List<DispatchResponse> getAll();

    DispatchResponse getById(Long id);

    DispatchResponse markAsDispatched(Long id);

    void delete(Long id);
}