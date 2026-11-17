package com.fishfarm.management.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FeedTypeRequest(
        @NotNull @Min(1) @Max(4) Integer sizeMm,
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal ratePerKg,
        @NotNull Boolean active) {}
