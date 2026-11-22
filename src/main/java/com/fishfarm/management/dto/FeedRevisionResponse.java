package com.fishfarm.management.dto;
import com.fishfarm.management.entity.FeedRevision;
import java.math.BigDecimal;
import java.time.Instant;
public record FeedRevisionResponse(Long id,long revision,String action,BigDecimal beforeQuantityKg,BigDecimal afterQuantityKg,
    BigDecimal beforeExpense,BigDecimal afterExpense,BigDecimal adjustment,String reason,Long actorId,String actorName,Instant reviewedAt) {
    public static FeedRevisionResponse from(FeedRevision r) {
        return new FeedRevisionResponse(r.getId(),r.getRevision(),r.getAction(),r.getBeforeQuantityKg(),r.getAfterQuantityKg(),r.getBeforeExpense(),r.getAfterExpense(),r.getAdjustment(),r.getReason(),r.getActorId(),r.getActorName(),r.getReviewedAt());
    }
}
