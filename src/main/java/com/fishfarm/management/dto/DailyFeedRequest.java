package com.fishfarm.management.dto;

import com.fishfarm.management.enums.FeedingTime;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyFeedRequest(
        @NotNull @Positive Long pondId,
        @NotNull @Positive Long feedTypeId,
        @NotNull @PastOrPresent LocalDate feedingDate,
        @NotNull FeedingTime feedingTime,
        @NotNull @DecimalMin("0.001") @Digits(integer = 7, fraction = 3) BigDecimal quantityKg,
        @Size(max = 500) String notes) {}
