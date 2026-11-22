package com.fishfarm.management.service.impl;
import com.fishfarm.management.dto.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.security.FarmContext;
import com.fishfarm.management.service.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.Clock;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FeedReviewServiceImpl implements FeedReviewService {
    private final DailyFeedRepository feeds;
    private final PondRepository ponds;
    private final FeedRevisionRepository revisions;
    private final AppAccountRepository accounts;
    private final BatchService batches;
    private final AuditService audit;
    private final Clock clock;
    private final EntityManager em;
    public List<FeedRevisionResponse> revisions(Long id) {
        feeds.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Feeding record not found"));
        return revisions.findByFeedIdOrderByRevisionAsc(id).stream().map(FeedRevisionResponse::from).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public DailyFeedResponse review(Long id,FeedReviewRequest request) {
        if(!"OWNER".equals(FarmContext.role()))throw error(HttpStatus.FORBIDDEN,"Only the farm owner can review feeding records");
        DailyFeed feed=feeds.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Feeding record not found"));
        // All feeding and batch mutations use the pond lock first. Refresh after
        // waiting so a second review cannot overwrite a newer committed revision.
        ponds.findForFeedingUpdate(feed.getPond().getId()).orElseThrow();
        em.refresh(feed,LockModeType.PESSIMISTIC_WRITE);
        if(feed.getVersion()!=request.expectedVersion())throw error(HttpStatus.CONFLICT,"This record changed. Reload its latest history before reviewing again.");
        String reason=request.reason().strip();
        if(reason.length()<5)throw error(HttpStatus.BAD_REQUEST,"Enter a meaningful correction reason (at least 5 characters)");
        var beforeQuantity=feed.netQuantity(); var beforeExpense=feed.netExpense();
        switch(request.action()) {
            case CANCEL -> {
                if(feed.cancelled())throw error(HttpStatus.CONFLICT,"This entry is already cancelled");
                feed.setStatus("CANCELLED");
            }
            case CORRECT, RESTORE -> {
                if(feed.cancelled() != (request.action()==FeedReviewRequest.Action.RESTORE))throw error(HttpStatus.CONFLICT,"Use Restore for a cancelled entry, or Correct for a recorded entry");
                if(request.quantityKg()==null || request.quantityKg().signum()<=0)throw error(HttpStatus.BAD_REQUEST,"Enter the actual quantity greater than zero");
                if(!feed.cancelled() && feed.getQuantityKg().compareTo(request.quantityKg())==0)throw error(HttpStatus.BAD_REQUEST,"The quantity is unchanged");
                feed.setQuantityKg(request.quantityKg());
                feed.setExpense(request.quantityKg().multiply(feed.getRatePerKg()).setScale(2,RoundingMode.HALF_UP));
                feed.setStatus("CORRECTED");
            }
        }
        feeds.flush();
        // Preserve the existing batch allocation and carry-forward guards. A
        // cancellation zeros its effective cost; it does not delete the ledger row.
        batches.syncFeed(feed);
        FeedRevision row=new FeedRevision();row.setFeed(feed);row.setRevision(feed.getVersion());row.setAction(request.action().name());
        row.setBeforeQuantityKg(beforeQuantity);row.setAfterQuantityKg(feed.netQuantity());row.setBeforeExpense(beforeExpense);row.setAfterExpense(feed.netExpense());row.setAdjustment(feed.netExpense().subtract(beforeExpense));
        row.setReason(reason);row.setActorId(FarmContext.userId());row.setActorName(accounts.findById(FarmContext.userId()).map(AppAccount::getDisplayName).orElse("Account "+FarmContext.userId()));row.setReviewedAt(clock.instant());
        revisions.saveAndFlush(row);
        audit.record(request.action()+"_FEED","DAILY_FEED",id,"Revision "+feed.getVersion()+"; adjustment "+row.getAdjustment()+"; reason: "+reason);
        return DailyFeedResponse.from(feed);
    }
    private ResponseStatusException error(HttpStatus status,String message) {return new ResponseStatusException(status,message);}
}
