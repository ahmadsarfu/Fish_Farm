package com.fishfarm.management.dto;

import com.fishfarm.management.entity.DailyFeed;
import com.fishfarm.management.enums.FeedingTime;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyFeedResponse(Long id, Long pondId, String pondName, Long feedTypeId,
        String feedName, Integer sizeMm, LocalDate feedingDate, FeedingTime feedingTime,
        BigDecimal quantityKg, BigDecimal ratePerKg, BigDecimal expense, String notes, String pondNumber,
        String status, String source, long version, BigDecimal originalQuantityKg, BigDecimal originalRatePerKg,
        BigDecimal originalExpense, BigDecimal netQuantityKg, BigDecimal netExpense) {
    public static DailyFeedResponse from(DailyFeed feed) {
        return new DailyFeedResponse(feed.getId(), feed.getPond().getId(), feed.getPond().getPondName(),
                feed.getFeedType().getId(), feed.getFeedType().getName(), feed.getFeedType().getSizeMm(),
                feed.getFeedingDate(), feed.getFeedingTime(), feed.getQuantityKg(), feed.getRatePerKg(),
                feed.getExpense(), feed.getNotes(), feed.getPond().getPondNumber(), feed.getStatus(), feed.getSource(), feed.getVersion(),
                feed.getOriginalQuantityKg(), feed.getOriginalRatePerKg(), feed.getOriginalExpense(), feed.netQuantity(), feed.netExpense());
    }
}
