package com.fishfarm.management.service;
import com.fishfarm.management.dto.*;
import java.util.List;
public interface FeedReviewService {
    DailyFeedResponse review(Long id,FeedReviewRequest request);
    List<FeedRevisionResponse> revisions(Long id);
}
