package com.fishfarm.management.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record FeedingSlotRequest(@NotNull @PastOrPresent LocalDate date, @NotNull Boolean completed) {}
