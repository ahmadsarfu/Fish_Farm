package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class BatchRequests {
    public record Create(@NotBlank @Size(max=40) String code,@NotBlank @Size(max=120) String name,
            @Positive Long stockEntryId,@Positive Long fishTypeId,@Positive Long pondId,
            @NotNull @PastOrPresent LocalDate date,@NotNull @Min(1) @Max(100000000) Integer count,
            @NotNull @DecimalMin("0") @Digits(integer=14,fraction=2) BigDecimal purchaseCost,
            @DecimalMin("0") @Digits(integer=14,fraction=2) BigDecimal historicalCost) {}
    public record Mortality(@NotNull UUID requestId,@NotNull @PastOrPresent LocalDate date,
            @NotNull @Min(1) @Max(100000000) Integer count,@NotBlank @Size(max=500) String reason) {}
    public record Transfer(@NotNull UUID requestId,@NotNull @PastOrPresent LocalDate date,
            @NotNull @Min(1) @Max(100000000) Integer count,@NotNull @Positive Long targetPondId,
            @NotNull @DecimalMin("0") @Digits(integer=14,fraction=2) BigDecimal transferCost,@Size(max=500) String notes) {}
    public record Expense(@NotNull UUID requestId,@NotNull @PastOrPresent LocalDate date,
            @NotBlank @Pattern(regexp="MEDICINE|LABOUR|OTHER") String category,
            @NotNull @DecimalMin("0.01") @Digits(integer=14,fraction=2) BigDecimal amount,@Size(max=500) String notes) {}
    public record Harvest(@NotNull UUID requestId,@NotNull @PastOrPresent LocalDate date,
            @NotNull @Min(1) @Max(100000000) Integer count,
            @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal weightKg,
            @NotNull @DecimalMin("0") @Digits(integer=14,fraction=2) BigDecimal revenue,@Size(max=500) String notes) {}
}
