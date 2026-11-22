package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record FeedReviewRequest(@NotNull Action action,@NotNull @Min(0) Long expectedVersion,
    @DecimalMin("0.001") @Digits(integer=7,fraction=3) BigDecimal quantityKg,
    @NotBlank @Size(min=5,max=500) String reason) {
    public enum Action { CORRECT, CANCEL, RESTORE }
}
