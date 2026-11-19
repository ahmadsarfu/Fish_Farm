package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
public final class TrialRequests {
    public record Submit(@NotBlank @Size(max=120) String ownerName,@NotBlank @Size(max=120) String farmName,
        @NotBlank @Size(max=30) String mobile,@Email @Size(max=254) String email) {}
    public record Approve(@NotBlank @Size(max=80) String planName,@Min(1) @Max(365) int trialDays,
        @Min(1) @Max(100000) int pondLimit,@Min(1) @Max(100000) int userLimit) {}
}
