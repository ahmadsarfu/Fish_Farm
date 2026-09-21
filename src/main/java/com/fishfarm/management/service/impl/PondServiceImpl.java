package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.PondRequest;
import com.fishfarm.management.dto.PondResponse;
import com.fishfarm.management.entity.Pond;
import com.fishfarm.management.enums.PondStatus;
import com.fishfarm.management.repository.PondRepository;
import com.fishfarm.management.service.PondService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PondServiceImpl implements PondService {

    private final PondRepository pondRepository;

    @Override
    public PondResponse create(PondRequest request) {

        if (pondRepository.existsByPondNumber(request.getPondNumber())) {
            throw new RuntimeException("Pond number already exists");
        }

        Pond pond = new Pond();
        pond.setPondNumber(request.getPondNumber());
        pond.setPondName(request.getPondName());
        pond.setAreaAcres(request.getAreaAcres());
        pond.setDescription(request.getDescription());

        Pond saved = pondRepository.save(pond);

        return map(saved);
    }

    @Override
    public List<PondResponse> getAll() {

        return pondRepository.findByDeletedFalse().stream().map(this::map).toList();
    }

    @Override
    public PondResponse getById(Long id) {

        Pond pond = pondRepository.findById(id).orElseThrow(() -> new RuntimeException("Pond not found"));

        return map(pond);
    }

    @Override
    public PondResponse update(Long id, PondRequest request) {

        Pond pond = pondRepository.findById(id).orElseThrow(() -> new RuntimeException("Pond not found"));

        pond.setPondName(request.getPondName());
        pond.setAreaAcres(request.getAreaAcres());
        pond.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            pond.setStatus(request.getStatus());
        }

        return map(pondRepository.save(pond));
    }

    @Override
    public void delete(Long id) {

        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pond not found"));

        pond.setStatus(PondStatus.INACTIVE);

        pondRepository.save(pond);
    }

    private PondResponse map(Pond pond) {

        return PondResponse.builder().id(pond.getId()).pondNumber(pond.getPondNumber()).pondName(pond.getPondName()).areaAcres(pond.getAreaAcres()).description(pond.getDescription()).status(pond.getStatus()).build();
    }
}
