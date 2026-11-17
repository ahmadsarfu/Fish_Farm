package com.fishfarm.management.service;

import com.fishfarm.management.dto.*;
import com.fishfarm.management.enums.FeedingTime;
import java.time.LocalDate;

public interface FeedingDashboardService {
    FeedingDashboardResponse dashboard(LocalDate date);
    FeedingDashboardResponse configure(Long pondId, PondFeedConfigRequest request, LocalDate date);
    FeedingDashboardResponse complete(Long pondId, FeedingTime time, FeedingSlotRequest request);
}
