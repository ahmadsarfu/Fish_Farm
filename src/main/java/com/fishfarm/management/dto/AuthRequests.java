package com.fishfarm.management.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public final class AuthRequests {
    public record Login(@NotBlank @Size(max=254) String username,@NotBlank @Size(max=72) String password,@Size(max=6) String otp) {}
    public record Register(@NotBlank @Size(max=120) String displayName,
            @NotBlank @Size(min=3,max=120) @Pattern(regexp="[A-Za-z0-9._@+\\-]+") String username,
            @NotBlank @Size(min=10,max=72) String password,
            @Size(max=120) String farmName,boolean claimExisting,String setupToken,
            @NotBlank @Size(max=30) String mobile,@Email @Size(max=254) String email) {}
    public record SelectFarm(@NotNull @Positive Long farmId) {}
    public record NewFarm(@NotBlank @Size(max=120) String name) {}
    public record NewMember(@NotBlank @Size(max=120) String displayName,
            @NotBlank @Size(min=3,max=120) @Pattern(regexp="[A-Za-z0-9._@+\\-]+") String username,
            @NotBlank @Size(min=10,max=72) String password,@NotBlank @Pattern(regexp="MANAGER|LABOUR") String role,
            @NotNull @Size(max=500) List<@Positive Long> pondIds) {}
    public record UpdateMember(@NotBlank @Pattern(regexp="MANAGER|LABOUR") String role, boolean active,
            @NotNull @Size(max=500) List<@Positive Long> pondIds) {}
    public record ChangePassword(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=72) String newPassword) {}
}
