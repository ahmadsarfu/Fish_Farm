package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record BatchSettlementRequest(@NotNull UUID requestId,@NotNull @PastOrPresent LocalDate date,
        @NotNull @DecimalMin("0.01") @Digits(integer=14,fraction=2) BigDecimal amount,
        @NotBlank @Size(max=500) String notes) {}
