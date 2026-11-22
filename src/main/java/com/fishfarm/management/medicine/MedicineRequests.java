package com.fishfarm.management.medicine;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class MedicineRequests {
 private MedicineRequests() {}
 public record Product(@NotBlank @Size(max=100) String name,@Size(max=60) String brand,
  @NotBlank @Size(max=40) String packLabel,@NotNull Unit unit,@JsonDeserialize(using=WholeUnitsDeserializer.class) @Min(0) @Max(1000000) int minimumStock,
  @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal sellingPrice,boolean active,@Min(0) Long expectedVersion) {}
 public enum Unit { BOTTLE, PACKET }
 public record Purchase(@NotNull UUID requestKey,@NotNull @Positive Long productId,
  @NotBlank @Size(max=50) String batchNumber,@NotNull LocalDate expiryDate,
  @NotNull @PastOrPresent LocalDate date,@JsonDeserialize(using=WholeUnitsDeserializer.class) @Min(1) @Max(1000000) int quantity,
  @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal unitCost,
  @Size(max=120) String supplier,@Size(max=300) String notes) {}
 public enum Kind { SALE, OWN_USE, SALES_RETURN, SUPPLIER_RETURN, DAMAGE }
 public record Movement(@NotNull UUID requestKey,@NotNull @Positive Long lotId,@NotNull Kind kind,
  @NotNull @PastOrPresent LocalDate date,@JsonDeserialize(using=WholeUnitsDeserializer.class) @Min(1) @Max(1000000) int quantity,
  @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal unitPrice,
  @Positive Long referenceId,@Size(max=120) String party,@Size(max=300) String notes) {}
 public record Reverse(@NotNull UUID requestKey,@NotBlank @Size(min=5,max=300) String reason) {}
}
