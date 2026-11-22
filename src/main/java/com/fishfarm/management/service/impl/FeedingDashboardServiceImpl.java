package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.*;
import com.fishfarm.management.dto.FeedingDashboardResponse.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.enums.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.FeedingDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedingDashboardServiceImpl implements FeedingDashboardService {
    private final PondRepository ponds;
    private final com.fishfarm.management.security.FarmAccess access;
    private final FeedTypeRepository types;
    private final PondFeedConfigRepository configs;
    private final DailyFeedRepository feeds;
    private final com.fishfarm.management.service.BatchService batchService;
    private final com.fishfarm.management.service.AuditService audit;
    private final java.time.Clock clock;

    @Override
    public FeedingDashboardResponse dashboard(LocalDate date) {
        List<DailyFeed> entries = feeds.search(date, date, null);
        Map<Long, List<DailyFeed>> byPond = entries.stream().collect(Collectors.groupingBy(f -> f.getPond().getId()));
        Map<Long, PondFeedConfig> setup = configs.findAllWithDetails().stream()
                .collect(Collectors.toMap(c -> c.getPond().getId(), Function.identity()));
        List<PondRow> rows = ponds.findAll().stream()
                .filter(p -> (active(p) || byPond.containsKey(p.getId())) && access.canFeed(p.getId()))
                .sorted(Comparator.comparing(Pond::getPondNumber).thenComparing(Pond::getId))
                .map(p -> {
                    PondFeedConfig c = setup.get(p.getId());
                    Config config = c == null ? null : new Config(c.getFeedType().getId(), c.getFeedType().getName(),
                            c.getFeedType().getSizeMm(), c.getFeedType().getRatePerKg(),
                            Boolean.TRUE.equals(c.getFeedType().getActive()), c.getMorningQtyKg(), c.getEveningQtyKg());
                    List<DailyFeed> records = byPond.getOrDefault(p.getId(), List.of());
                    return new PondRow(p.getId(), p.getPondNumber(), p.getPondName(), active(p), config,
                            slot(records, FeedingTime.MORNING), slot(records, FeedingTime.EVENING));
                }).toList();
        int completed = 0, planned = 0;
        for (PondRow row : rows) {
            if (row.morning().completed()) completed++;
            if (row.evening().completed()) completed++;
            if (row.morning().completed() || (row.active() && row.config() != null && positive(row.config().morningQtyKg()))) planned++;
            if (row.evening().completed() || (row.active() && row.config() != null && positive(row.config().eveningQtyKg()))) planned++;
        }
        BigDecimal expense = entries.stream().filter(f -> access.canFeed(f.getPond().getId())).map(DailyFeed::netExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FeedingDashboardResponse(date, completed, planned, expense, rows);
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public FeedingDashboardResponse configure(Long pondId, PondFeedConfigRequest request, LocalDate date) {
        Pond pond = lock(pondId);
        if (!active(pond)) throw bad("Select an active pond");
        FeedType type = types.findById(request.feedTypeId()).orElseThrow(() -> missing("Feed type not found"));
        if (!Boolean.TRUE.equals(type.getActive())) throw bad("Select an active feed");
        if (!positive(request.morningQtyKg()) && !positive(request.eveningQtyKg()))
            throw bad("Set a quantity greater than zero for at least one feeding time");
        PondFeedConfig config = configs.findByPondId(pondId).orElseGet(PondFeedConfig::new);
        config.setPond(pond); config.setFeedType(type);
        config.setMorningQtyKg(request.morningQtyKg()); config.setEveningQtyKg(request.eveningQtyKg());
        configs.saveAndFlush(config);
        return dashboard(date);
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public FeedingDashboardResponse complete(Long pondId, FeedingTime time, FeedingSlotRequest request) {
        access.requireFeed(pondId);
        if ("LABOUR".equals(com.fishfarm.management.security.FarmContext.role()) && (!request.completed() || !request.date().equals(LocalDate.now(clock))))
            throw bad("Labour can mark today's feeding only. Ask a manager for corrections.");
        Pond pond = lock(pondId);
        List<DailyFeed> existing = feeds.search(request.date(), request.date(), pondId).stream()
                .filter(f -> f.getFeedingTime() == time).toList();
        if (!request.completed()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Saved ticks are locked. The owner can correct or cancel them in Feeding History.");
        if (existing.stream().anyMatch(feed -> !feed.cancelled())) return dashboard(request.date());
        if (!existing.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"This slot has a cancelled record. Ask the owner to review and restore it.");
        if (!active(pond)) throw bad("This pond is inactive");
        PondFeedConfig config = configs.findByPondId(pondId).orElseThrow(() -> bad("Set up feed and quantity for this pond first"));
        FeedType type = config.getFeedType();
        BigDecimal quantity = time == FeedingTime.MORNING ? config.getMorningQtyKg() : config.getEveningQtyKg();
        if (!Boolean.TRUE.equals(type.getActive())) throw bad("This feed is inactive. Update pond setup first");
        if (!positive(quantity)) throw bad("No feed quantity is planned for this time");
        if (!positive(type.getRatePerKg())) throw bad("Set a feed rate greater than zero in Feed Master first");
        DailyFeed feed = new DailyFeed(); feed.setPond(pond); feed.setFeedType(type);
        feed.setFeedingDate(request.date()); feed.setFeedingTime(time);
        feed.setSource("CHECKLIST");
        feed.setQuantityKg(quantity); feed.setRatePerKg(type.getRatePerKg());
        feed.setExpense(quantity.multiply(type.getRatePerKg()).setScale(2, RoundingMode.HALF_UP));
        feed=feeds.saveAndFlush(feed);
        batchService.syncFeed(feed);
        audit.record("COMPLETE_FEED","DAILY_FEED",feed.getId(),time+" "+quantity+" kg; expense "+feed.getExpense());
        return dashboard(request.date());
    }

    private Slot slot(List<DailyFeed> entries, FeedingTime time) {
        List<DailyFeed> selected = entries.stream().filter(f -> f.getFeedingTime() == time).toList();
        return new Slot(selected.stream().anyMatch(f -> !f.cancelled()), !selected.isEmpty() && selected.stream().allMatch(DailyFeed::cancelled), selected.stream().map(DailyFeed::netQuantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                selected.stream().map(DailyFeed::netExpense).reduce(BigDecimal.ZERO, BigDecimal::add),
                selected.stream().map(DailyFeedResponse::from).toList());
    }
    private Pond lock(Long id) { return ponds.findForFeedingUpdate(id).orElseThrow(() -> missing("Pond not found")); }
    private boolean active(Pond p) { return !Boolean.TRUE.equals(p.getDeleted()) && p.getStatus() == PondStatus.ACTIVE; }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
