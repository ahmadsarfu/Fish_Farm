package com.fishfarm.management.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PondFeedConfigRequest(
        @NotNull @Positive Long feedTypeId,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 3) BigDecimal morningQtyKg,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 3) BigDecimal eveningQtyKg) {}
