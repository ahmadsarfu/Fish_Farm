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
    private final FeedTypeRepository types;
    private final PondFeedConfigRepository configs;
    private final DailyFeedRepository feeds;

    @Override
    public FeedingDashboardResponse dashboard(LocalDate date) {
        List<DailyFeed> entries = feeds.search(date, date, null);
        Map<Long, List<DailyFeed>> byPond = entries.stream().collect(Collectors.groupingBy(f -> f.getPond().getId()));
        Map<Long, PondFeedConfig> setup = configs.findAllWithDetails().stream()
                .collect(Collectors.toMap(c -> c.getPond().getId(), Function.identity()));
        List<PondRow> rows = ponds.findAll().stream()
                .filter(p -> active(p) || byPond.containsKey(p.getId()))
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
        BigDecimal expense = entries.stream().map(DailyFeed::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FeedingDashboardResponse(date, completed, planned, expense, rows);
    }

    @Override
    @Transactional
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
    @Transactional
    public FeedingDashboardResponse complete(Long pondId, FeedingTime time, FeedingSlotRequest request) {
        Pond pond = lock(pondId);
        List<DailyFeed> existing = feeds.search(request.date(), request.date(), pondId).stream()
                .filter(f -> f.getFeedingTime() == time).toList();
        if (!request.completed()) {
            feeds.deleteAll(existing); feeds.flush();
            return dashboard(request.date());
        }
        // Retried taps preserve the original quantity/rate and never create another charge.
        if (!existing.isEmpty()) return dashboard(request.date());
        if (!active(pond)) throw bad("This pond is inactive");
        PondFeedConfig config = configs.findByPondId(pondId).orElseThrow(() -> bad("Set up feed and quantity for this pond first"));
        FeedType type = config.getFeedType();
        BigDecimal quantity = time == FeedingTime.MORNING ? config.getMorningQtyKg() : config.getEveningQtyKg();
        if (!Boolean.TRUE.equals(type.getActive())) throw bad("This feed is inactive. Update pond setup first");
        if (!positive(quantity)) throw bad("No feed quantity is planned for this time");
        if (!positive(type.getRatePerKg())) throw bad("Set a feed rate greater than zero in Feed Master first");
        DailyFeed feed = new DailyFeed(); feed.setPond(pond); feed.setFeedType(type);
        feed.setFeedingDate(request.date()); feed.setFeedingTime(time);
        feed.setQuantityKg(quantity); feed.setRatePerKg(type.getRatePerKg());
        feed.setExpense(quantity.multiply(type.getRatePerKg()).setScale(2, RoundingMode.HALF_UP));
        feeds.saveAndFlush(feed);
        return dashboard(request.date());
    }

    private Slot slot(List<DailyFeed> entries, FeedingTime time) {
        List<DailyFeed> selected = entries.stream().filter(f -> f.getFeedingTime() == time).toList();
        return new Slot(!selected.isEmpty(), selected.stream().map(DailyFeed::getQuantityKg).reduce(BigDecimal.ZERO, BigDecimal::add),
                selected.stream().map(DailyFeed::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add),
                selected.stream().map(DailyFeedResponse::from).toList());
    }
    private Pond lock(Long id) { return ponds.findForFeedingUpdate(id).orElseThrow(() -> missing("Pond not found")); }
    private boolean active(Pond p) { return !Boolean.TRUE.equals(p.getDeleted()) && p.getStatus() == PondStatus.ACTIVE; }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
