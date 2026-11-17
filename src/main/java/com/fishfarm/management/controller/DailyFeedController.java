package com.fishfarm.management.controller;

import com.fishfarm.management.dto.*;
import com.fishfarm.management.entity.FeedType;
import com.fishfarm.management.service.DailyFeedService;
import com.fishfarm.management.service.FeedingDashboardService;
import com.fishfarm.management.enums.FeedingTime;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DailyFeedController {
    private final DailyFeedService service;
    private final FeedingDashboardService dashboardService;

    @GetMapping("/feeding-dashboard")
    public FeedingDashboardResponse dashboard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardService.dashboard(date);
    }

    @PutMapping("/pond-feed-configs/{pondId}")
    public FeedingDashboardResponse configure(@PathVariable Long pondId, @Valid @RequestBody PondFeedConfigRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardService.configure(pondId, request, date);
    }

    @PutMapping("/feeding-dashboard/{pondId}/slots/{time}")
    public FeedingDashboardResponse complete(@PathVariable Long pondId, @PathVariable FeedingTime time,
            @Valid @RequestBody FeedingSlotRequest request) {
        return dashboardService.complete(pondId, time, request);
    }

    @GetMapping("/feed-types")
    public List<FeedType> types() { return service.types(); }

    @PostMapping("/feed-types") @ResponseStatus(HttpStatus.CREATED)
    public FeedType createType(@Valid @RequestBody FeedTypeRequest request) { return service.saveType(null, request); }

    @PutMapping("/feed-types/{id}")
    public FeedType updateType(@PathVariable Long id, @Valid @RequestBody FeedTypeRequest request) { return service.saveType(id, request); }

    @GetMapping("/daily-feeds")
    public List<DailyFeedResponse> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long pondId) { return service.search(from, to, pondId); }

    @GetMapping("/daily-feeds/summary")
    public Map<String, Object> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long pondId) {
        List<DailyFeedResponse> entries = service.search(from, to, pondId);
        return Map.of("entryCount", entries.size(),
                "totalQuantityKg", entries.stream().map(DailyFeedResponse::quantityKg).reduce(BigDecimal.ZERO, BigDecimal::add),
                "totalExpense", entries.stream().map(DailyFeedResponse::expense).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @PostMapping("/daily-feeds") @ResponseStatus(HttpStatus.CREATED)
    public DailyFeedResponse create(@Valid @RequestBody DailyFeedRequest request) { return service.save(null, request); }

    @PutMapping("/daily-feeds/{id}")
    public DailyFeedResponse update(@PathVariable Long id, @Valid @RequestBody DailyFeedRequest request) { return service.save(id, request); }

    @DeleteMapping("/daily-feeds/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statusError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", Objects.requireNonNullElse(exception.getReason(), "Request failed")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "A matching feed record already exists or this record is in use. Refresh and try again."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right).orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
