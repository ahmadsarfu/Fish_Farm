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
        DailyFeed feed = id == null ? new DailyFeed() : feeds.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Feeding record not found"));
        Pond pond = ponds.findForFeedingUpdate(request.pondId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pond not found"));
        FeedType type = types.findById(request.feedTypeId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Feed type not found"));
        boolean samePond = id != null && feed.getPond().getId().equals(pond.getId());
        boolean sameType = id != null && feed.getFeedType().getId().equals(type.getId());
        if ((!samePond && (Boolean.TRUE.equals(pond.getDeleted()) || pond.getStatus() != PondStatus.ACTIVE))
                || (!sameType && !Boolean.TRUE.equals(type.getActive())))
            throw error(HttpStatus.BAD_REQUEST, "Select an active pond and feed type");
        if (feeds.existsByPondIdAndFeedingDateAndFeedingTimeAndFeedTypeIdAndIdNot(pond.getId(),
                request.feedingDate(), request.feedingTime(), type.getId(), id == null ? -1L : id))
            throw error(HttpStatus.CONFLICT, "This pond already has this feed for the selected date and time. Edit the existing record.");
        if (!sameType) feed.setRatePerKg(type.getRatePerKg());
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
        DailyFeed feed = feeds.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Feeding record not found"));
        ponds.findForFeedingUpdate(feed.getPond().getId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pond not found"));
        batchService.removeFeed(feed);
        audit.record("DELETE_FEED","DAILY_FEED",feed.getId(),"Removed feeding expense "+feed.getExpense());
        feeds.delete(feed);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
