package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class PlatformRequests {
    public record Invite(@NotBlank @Size(max=120) String ownerName,@NotBlank @Size(max=120) String farmName,
            @NotBlank @Size(max=30) String mobile,@Email @Size(max=254) String email,
            @NotBlank @Size(max=80) String planName,@Min(1) @Max(365) int trialDays,
            @Min(1) @Max(100000) int pondLimit,@Min(1) @Max(100000) int userLimit) {}
    public record Subscription(@NotBlank @Pattern(regexp="TRIAL|ACTIVE|SUSPENDED") String status,
            @NotBlank @Size(max=80) String planName,LocalDate endsOn,
            @Min(1) @Max(100000) int pondLimit,@Min(1) @Max(100000) int userLimit) {}
    public record InvitationToken(@NotBlank @Size(min=40,max=64) String token) {}
    public record Activate(@NotBlank @Size(min=40,max=64) String token,@NotBlank @Size(min=10,max=72) String password) {}
}
