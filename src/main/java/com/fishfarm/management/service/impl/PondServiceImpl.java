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
    private final com.fishfarm.management.security.FarmAccess access;
    private final com.fishfarm.management.repository.BatchStageRepository stages;

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

        return pondRepository.findByDeletedFalse().stream().filter(p -> access.canFeed(p.getId())).map(this::map).toList();
    }

    @Override
    public PondResponse getById(Long id) {

        Pond pond = pondRepository.findById(id).orElseThrow(() -> new RuntimeException("Pond not found"));

        return map(pond);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PondResponse update(Long id, PondRequest request) {

        Pond pond = pondRepository.findForFeedingUpdate(id).orElseThrow(() -> new RuntimeException("Pond not found"));

        pond.setPondName(request.getPondName());
        pond.setAreaAcres(request.getAreaAcres());
        pond.setDescription(request.getDescription());

        if(request.getStatus()==PondStatus.INACTIVE && stages.findByPondIdAndCurrentCountGreaterThan(id,0).isPresent())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Transfer or harvest the active batch before deactivating this pond");
        if (request.getStatus() != null) {
            pond.setStatus(request.getStatus());
        }

        return map(pondRepository.save(pond));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        pondRepository.findForFeedingUpdate(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Pond not found"));
        if(stages.findByPondIdAndCurrentCountGreaterThan(id,0).isPresent())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Transfer or harvest the active batch before deactivating this pond");

        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pond not found"));

        pond.setStatus(PondStatus.INACTIVE);

        pondRepository.save(pond);
    }

    private PondResponse map(Pond pond) {

        return PondResponse.builder().id(pond.getId()).pondNumber(pond.getPondNumber()).pondName(pond.getPondName()).areaAcres(pond.getAreaAcres()).description(pond.getDescription()).status(pond.getStatus()).build();
    }
}
