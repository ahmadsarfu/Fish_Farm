package com.fishfarm.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FeedingDashboardResponse(LocalDate date, int completedSlots, int plannedSlots,
        BigDecimal totalExpense, List<PondRow> ponds) {
    public record Slot(boolean completed, boolean cancelled, BigDecimal quantityKg, BigDecimal expense,
                       List<DailyFeedResponse> entries) {}
    public record Config(Long feedTypeId, String feedName, Integer sizeMm, BigDecimal ratePerKg,
                         boolean feedActive, BigDecimal morningQtyKg, BigDecimal eveningQtyKg) {}
    public record PondRow(Long pondId, String pondNumber, String pondName, boolean active,
                          Config config, Slot morning, Slot evening) {}
}
