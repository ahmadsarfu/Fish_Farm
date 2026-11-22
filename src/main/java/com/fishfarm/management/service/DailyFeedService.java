package com.fishfarm.management.service;

import com.fishfarm.management.dto.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.enums.PondStatus;
import com.fishfarm.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyFeedService {
    private final FeedTypeRepository types;
    private final DailyFeedRepository feeds;
    private final PondRepository ponds;
    private final BatchService batchService;
    private final AuditService audit;

    public List<FeedType> types() { return types.findAllByOrderBySizeMmAsc(); }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public FeedType saveType(Long id, FeedTypeRequest request) {
        if (types.existsBySizeMmAndIdNot(request.sizeMm(), id == null ? -1L : id))
            throw error(HttpStatus.CONFLICT, "This feed size already exists. Edit its rate instead.");
        FeedType type = id == null ? new FeedType() : types.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Feed type not found"));
        // Size is the identity of a master record; changing it would relabel historical entries.
        if (id != null && !type.getSizeMm().equals(request.sizeMm()))
            throw error(HttpStatus.BAD_REQUEST, "Feed size cannot be changed after creation");
        type.setSizeMm(request.sizeMm());
        type.setName(request.name().trim());
        type.setRatePerKg(request.ratePerKg());
        type.setActive(request.active());
        return types.saveAndFlush(type);
    }

    public List<DailyFeedResponse> search(LocalDate from, LocalDate to, Long pondId) {
        if (from.isAfter(to)) throw error(HttpStatus.BAD_REQUEST, "From date must be on or before to date");
        return feeds.search(from, to, pondId).stream().map(DailyFeedResponse::from).toList();
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DailyFeedResponse save(Long id, DailyFeedRequest request) {
        if (!"OWNER".equals(com.fishfarm.management.security.FarmContext.role())) throw error(HttpStatus.FORBIDDEN,"Only the farm owner can add missed feeding records");
        if (id != null) throw error(HttpStatus.CONFLICT,"Saved feeding records are locked. Use owner review with a reason.");
        if (request.notes()==null || request.notes().strip().length()<5) throw error(HttpStatus.BAD_REQUEST,"Enter a reason for this missed feeding (at least 5 characters)");
        DailyFeed feed = new DailyFeed();
        Pond pond = ponds.findForFeedingUpdate(request.pondId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pond not found"));
        FeedType type = types.findById(request.feedTypeId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Feed type not found"));
        if (Boolean.TRUE.equals(pond.getDeleted()) || pond.getStatus() != PondStatus.ACTIVE
                || !Boolean.TRUE.equals(type.getActive()))
            throw error(HttpStatus.BAD_REQUEST, "Select an active pond and feed type");
        if (feeds.existsByPondIdAndFeedingDateAndFeedingTimeAndFeedTypeIdAndIdNot(pond.getId(),
                request.feedingDate(), request.feedingTime(), type.getId(), -1L))
            throw error(HttpStatus.CONFLICT, "This pond already has this feed for the selected date and time. Use owner review on the existing record.");
        feed.setRatePerKg(type.getRatePerKg());
        feed.setSource("MISSED_ENTRY");
        feed.setPond(pond);
        feed.setFeedType(type);
        feed.setFeedingDate(request.feedingDate());
        feed.setFeedingTime(request.feedingTime());
        feed.setQuantityKg(request.quantityKg());
        feed.setExpense(request.quantityKg().multiply(feed.getRatePerKg()).setScale(2, RoundingMode.HALF_UP));
        feed.setNotes(request.notes() == null ? null : request.notes().trim());
        feed=feeds.saveAndFlush(feed);
        batchService.syncFeed(feed);
        audit.record("SAVE_FEED","DAILY_FEED",feed.getId(),feed.getFeedingTime()+" "+feed.getQuantityKg()+" kg; expense "+feed.getExpense());
        return DailyFeedResponse.from(feed);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(Long id) {
        throw error(HttpStatus.CONFLICT,"Feeding records cannot be deleted. Use owner review to cancel with a reason.");
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
