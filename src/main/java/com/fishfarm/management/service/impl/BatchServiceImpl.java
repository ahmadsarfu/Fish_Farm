package com.fishfarm.management.service.impl;
import com.fishfarm.management.dto.*;
import com.fishfarm.management.dto.BatchRequests.*;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.enums.PondStatus;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class BatchServiceImpl implements BatchService {
    private final FishBatchRepository batches;
    private final BatchStageRepository stages;
    private final StageExpenseRepository expenses;
    private final BatchMovementRepository movements;
    private final PondRepository ponds;
    private final FishTypeRepository fishTypes;
    private final FishStockEntryRepository stock;
    private final HarvestService harvestService;
    private final AuditService audit;
    private final EntityManager entityManager;
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Batch or stage not found"); }
    private Map<String,Object> row(Object... values) { Map<String,Object> row=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)row.put((String)values[i],values[i+1]);return row; }
    private BigDecimal sum(List<StageExpense> values) { return values.stream().map(StageExpense::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add); }
    private BigDecimal remainingCost(BatchStage stage) {
        return stage.getOpeningCost().add(sum(expenses.findByStageId(stage.getId()))).subtract(stage.getTransferredCost()).subtract(stage.getHarvestedCost());
    }
    private void date(BatchStage stage,LocalDate date) {
        if(date.isBefore(stage.getStartedOn()) || date.isBefore(stage.getLastActivityOn())) throw bad("Date cannot be before the latest feeding, expense or movement for this stage");
    }
    private void open(BatchStage stage) { if(stage.getCurrentCount()<=0) throw bad("This stage has no remaining fish"); }
    private void count(BatchStage stage,int count) { if(count>stage.getCurrentCount())throw bad("Fish count exceeds the remaining "+stage.getCurrentCount()); }
    private Pond pond(Long id) { return ponds.findForFeedingUpdate(id).orElseThrow(()->bad("Pond not found")); }
    private void active(Pond pond) { if(Boolean.TRUE.equals(pond.getDeleted()) || pond.getStatus()!=PondStatus.ACTIVE)throw bad("Select an active pond"); }
    private BatchStage locked(Long id) {
        BatchStage stage=stages.findById(id).orElseThrow(this::missing);pond(stage.getPond().getId());entityManager.refresh(stage,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);return stage;
    }
    private void stageEmpty(Long pondId) { if(stages.findByPondIdAndCurrentCountGreaterThan(pondId,0).isPresent())throw bad("This pond already has an active batch. Use an empty pond"); }
    private String fingerprint(String kind,Long id,Object body) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((kind+":"+id+":"+body).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private Map<String,Object> replay(UUID requestId,String fingerprint) {
        var previous=movements.findByRequestId(requestId.toString());
        if(previous.isEmpty())return null;
        if(!previous.get().getFingerprint().equals(fingerprint))throw new ResponseStatusException(HttpStatus.CONFLICT,"This request key was already used with different details");
        return details(previous.get().getStage().getBatch().getId());
    }
    private BatchMovement movement(BatchStage stage,UUID key,String hash,String kind,LocalDate date,int count,String notes) {
        BatchMovement event=new BatchMovement();event.setStage(stage);event.setRequestId(key.toString());event.setFingerprint(hash);event.setKind(kind);event.setEventDate(date);event.setFishCount(count);event.setNotes(notes);
        stage.setLastActivityOn(date);return event;
    }
    private void addExpense(BatchStage stage,LocalDate date,String category,BigDecimal amount,String notes,Long feedId) {
        StageExpense expense=new StageExpense();expense.setStage(stage);expense.setExpenseDate(date);expense.setCategory(category);expense.setAmount(amount);expense.setNotes(notes);expense.setFeedEntryId(feedId);expenses.saveAndFlush(expense);stage.setLastActivityOn(date);
    }
    private BigDecimal allocation(BatchStage stage,int count) {
        BigDecimal available=remainingCost(stage);if(available.signum()<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Stage cost balance is invalid");
        return count==stage.getCurrentCount()?available:available.multiply(BigDecimal.valueOf(count)).divide(BigDecimal.valueOf(stage.getCurrentCount()),2,RoundingMode.HALF_UP);
    }
    private void closeIfEmpty(BatchStage stage,LocalDate date) { if(stage.getCurrentCount()==0)stage.setEndedOn(date); }
    @Override public List<Map<String,Object>> list() { return batches.findAllByOrderByStartedOnDescIdDesc().stream().map(b->summary(b,stages.findByBatchIdOrderByIdAsc(b.getId()),expenses.findByStageBatchIdOrderByExpenseDateAscIdAsc(b.getId()),movements.findByStageBatchIdOrderByEventDateAscIdAsc(b.getId()))).toList(); }
    private Map<String,Object> summary(FishBatch batch,List<BatchStage> stages,List<StageExpense> costs,List<BatchMovement> events) {
        BigDecimal expense=batch.getPurchaseCost().add(sum(costs));
        BigDecimal revenue=events.stream().map(BatchMovement::getRevenue).reduce(BigDecimal.ZERO,BigDecimal::add);
        int remaining=stages.stream().mapToInt(BatchStage::getCurrentCount).sum(),deaths=stages.stream().mapToInt(BatchStage::getDeadCount).sum();
        BigDecimal weight=events.stream().map(BatchMovement::getWeightKg).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        return row("harvestedWeightKg",weight,"finalCostPerKg",remaining==0 && weight.signum()>0?expense.divide(weight,2,RoundingMode.HALF_UP):null,"nurseryExpense",batch.getPurchaseCost().add(sum(costs.stream().filter(c->c.getStage().getStageNumber()==1).toList())),"transferExpense",sum(costs.stream().filter(c->c.getCategory().equals("TRANSFER")).toList()),"growOutExpense",sum(costs.stream().filter(c->c.getStage().getStageNumber()==2 && !c.getCategory().equals("TRANSFER")).toList()),"id",batch.getId(),"code",batch.getCode(),"name",batch.getName(),"fishTypeId",batch.getFishType().getId(),"fishTypeName",batch.getFishType().getName(),"stockEntryId",batch.getStockEntry()==null?null:batch.getStockEntry().getId(),"startedOn",batch.getStartedOn(),"initialCount",batch.getInitialCount(),"remainingCount",remaining,"deadCount",deaths,"harvestedCount",stages.stream().mapToInt(BatchStage::getHarvestedCount).sum(),"purchaseCost",batch.getPurchaseCost(),"totalExpense",expense,"revenue",revenue,"closed",remaining==0,"finalProfit",remaining==0?revenue.subtract(expense):null);
    }
    @Override public Map<String,Object> details(Long id) {
        FishBatch batch=batches.findById(id).orElseThrow(this::missing);
        List<BatchStage> stageList=stages.findByBatchIdOrderByIdAsc(id);List<StageExpense> costs=expenses.findByStageBatchIdOrderByExpenseDateAscIdAsc(id);List<BatchMovement> events=movements.findByStageBatchIdOrderByEventDateAscIdAsc(id);
        Map<String,Object> result=summary(batch,stageList,costs,events);
        result.put("stages",stageList.stream().map(s->{
            BigDecimal added=sum(costs.stream().filter(c->c.getStage().getId().equals(s.getId())).toList());BigDecimal balance=s.getOpeningCost().add(added).subtract(s.getTransferredCost()).subtract(s.getHarvestedCost());
            return row("id",s.getId(),"parentStageId",s.getParent()==null?null:s.getParent().getId(),"pondId",s.getPond().getId(),"pondName",s.getPond().getPondName(),"stageNumber",s.getStageNumber(),"label",s.getStageNumber()==1?"Nursery":"Grow-out","startedOn",s.getStartedOn(),"endedOn",s.getEndedOn(),"lastActivityOn",s.getLastActivityOn(),"openingCount",s.getOpeningCount(),"remainingCount",s.getCurrentCount(),"deadCount",s.getDeadCount(),"transferredCount",s.getTransferredCount(),"harvestedCount",s.getHarvestedCount(),"openingCost",s.getOpeningCost(),"addedCost",added,"feedCost",costs.stream().filter(c->c.getStage().getId().equals(s.getId())&&c.getCategory().equals("FEED")).map(StageExpense::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add),"carriedOutCost",s.getTransferredCost(),"harvestedCost",s.getHarvestedCost(),"remainingCost",balance,"costPerRemainingFish",s.getCurrentCount()>0?balance.divide(BigDecimal.valueOf(s.getCurrentCount()),2,RoundingMode.HALF_UP):null,"lossCost",s.getCurrentCount()==0?balance:BigDecimal.ZERO);
        }).toList());
        result.put("expenses",costs.stream().map(c->row("id",c.getId(),"stageId",c.getStage().getId(),"date",c.getExpenseDate(),"category",c.getCategory(),"amount",c.getAmount(),"feedEntryId",c.getFeedEntryId(),"notes",c.getNotes(),"recordedBy",c.getRecordedBy())).toList());
        result.put("movements",events.stream().map(e->row("id",e.getId(),"stageId",e.getStage().getId(),"targetStageId",e.getTarget()==null?null:e.getTarget().getId(),"kind",e.getKind(),"date",e.getEventDate(),"count",e.getFishCount(),"carriedCost",e.getAllocatedCost(),"weightKg",e.getWeightKg(),"revenue",e.getRevenue(),"harvestId",e.getHarvestId(),"notes",e.getNotes(),"recordedBy",e.getRecordedBy())).toList());return result;
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> create(Create request) {
        if(batches.existsByCode(request.code().trim()))throw bad("Batch code already exists");
        FishStockEntry entry=null;Pond pond;FishType type;BigDecimal purchase;int count=request.count();
        if(request.stockEntryId()!=null) {
            entry=stock.findByIdAndDeletedFalse(request.stockEntryId()).orElseThrow(()->bad("Stock entry not found"));
            if(batches.existsByStockEntryId(entry.getId()))throw bad("This stock entry already has a batch");
            pond=pond(entry.getPond().getId());type=entry.getFishType();purchase=entry.getPurchaseAmount();
            if(entry.getQuantity()!=null && entry.getQuantity()!=count)throw bad("Initial count must match the stock entry quantity");
            if(!request.date().equals(entry.getStockDate()))throw bad("Batch start date must match the stock entry date");
        } else {
            if(request.pondId()==null || request.fishTypeId()==null)throw bad("Pond and fish type are required");
            pond=pond(request.pondId());type=fishTypes.findById(request.fishTypeId()).orElseThrow(()->bad("Fish type not found"));purchase=request.purchaseCost();
        }
        active(pond);stageEmpty(pond.getId());
        if(entry==null) {entry=new FishStockEntry();entry.setPond(pond);entry.setFishType(type);entry.setStockDate(request.date());entry.setQuantity(count);entry.setPurchaseAmount(purchase);entry=stock.saveAndFlush(entry);}
        FishBatch batch=new FishBatch();batch.setCode(request.code().trim());batch.setName(request.name().trim());batch.setFishType(type);batch.setStockEntry(entry);batch.setStartedOn(request.date());batch.setInitialCount(count);batch.setPurchaseCost(purchase);batch=batches.saveAndFlush(batch);
        BatchStage stage=new BatchStage();stage.setBatch(batch);stage.setPond(pond);stage.setStageNumber(1);stage.setStartedOn(request.date());stage.setLastActivityOn(request.date());stage.setOpeningCount(count);stage.setCurrentCount(count);stage.setOpeningCost(purchase);stages.saveAndFlush(stage);
        if(request.historicalCost()!=null && request.historicalCost().signum()>0)addExpense(stage,request.date(),"OPENING",request.historicalCost(),"Historical growth cost before batch tracking",null);
        audit.record("CREATE_BATCH","BATCH",batch.getId(),"Created nursery batch with "+count+" fish");return details(batch.getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> mortality(Long id,Mortality request) {
        BatchStage s=locked(id);String hash=fingerprint("MORTALITY",id,request);var replay=replay(request.requestId(),hash);if(replay!=null)return replay;
        open(s);date(s,request.date());count(s,request.count());
        s.setCurrentCount(s.getCurrentCount()-request.count());s.setDeadCount(s.getDeadCount()+request.count());closeIfEmpty(s,request.date());
        movements.saveAndFlush(movement(s,request.requestId(),hash,"MORTALITY",request.date(),request.count(),request.reason()));
        audit.record("MORTALITY","BATCH_STAGE",id,request.count()+" fish lost: "+request.reason());return details(s.getBatch().getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> expense(Long id,Expense request) {
        BatchStage s=locked(id);String hash=fingerprint("EXPENSE",id,request);var replay=replay(request.requestId(),hash);if(replay!=null)return replay;
        open(s);date(s,request.date());addExpense(s,request.date(),request.category(),request.amount(),request.notes(),null);
        movements.saveAndFlush(movement(s,request.requestId(),hash,"EXPENSE",request.date(),0,request.notes()));
        audit.record("ADD_EXPENSE","BATCH_STAGE",id,request.category()+": "+request.amount());return details(s.getBatch().getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> transfer(Long id,Transfer request) {
        BatchStage s=stages.findById(id).orElseThrow(this::missing);Long source=s.getPond().getId();
        if(source.equals(request.targetPondId()))throw bad("Choose a different destination pond");
        for(Long pondId:new TreeSet<>(List.of(source,request.targetPondId())))pond(pondId);
        entityManager.refresh(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);String hash=fingerprint("TRANSFER",id,request);var replay=replay(request.requestId(),hash);if(replay!=null)return replay;
        open(s);date(s,request.date());count(s,request.count());Pond target=pond(request.targetPondId());active(target);stageEmpty(target.getId());
        BigDecimal carry=allocation(s,request.count());
        BatchStage child=new BatchStage();child.setBatch(s.getBatch());child.setPond(target);child.setParent(s);child.setStageNumber(2);child.setStartedOn(request.date());child.setLastActivityOn(request.date());child.setOpeningCount(request.count());child.setCurrentCount(request.count());child.setOpeningCost(carry);
        s.setCurrentCount(s.getCurrentCount()-request.count());s.setTransferredCount(s.getTransferredCount()+request.count());s.setTransferredCost(s.getTransferredCost().add(carry));closeIfEmpty(s,request.date());
        stages.saveAndFlush(s);child=stages.saveAndFlush(child);
        if(request.transferCost().signum()>0)addExpense(child,request.date(),"TRANSFER",request.transferCost(),request.notes(),null);
        BatchMovement event=movement(s,request.requestId(),hash,"TRANSFER",request.date(),request.count(),request.notes());event.setTarget(child);event.setAllocatedCost(carry);movements.saveAndFlush(event);
        audit.record("TRANSFER","BATCH_STAGE",id,"Transferred "+request.count()+" fish to pond "+target.getId()+"; carried cost "+carry);return details(s.getBatch().getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> harvest(Long id,com.fishfarm.management.dto.BatchRequests.Harvest request) {
        BatchStage s=locked(id);String hash=fingerprint("HARVEST",id,request);var replay=replay(request.requestId(),hash);if(replay!=null)return replay;
        open(s);date(s,request.date());count(s,request.count());BigDecimal carry=allocation(s,request.count());
        HarvestRequest harvest=new HarvestRequest();harvest.setPondId(s.getPond().getId());harvest.setHarvestDate(request.date());harvest.setRemarks("Batch "+s.getBatch().getCode()+": "+Objects.toString(request.notes(),""));
        HarvestItemRequest item=new HarvestItemRequest();item.setFishTypeId(s.getBatch().getFishType().getId());item.setWeightKg(request.weightKg());harvest.setItems(List.of(item));
        HarvestResponse saved=harvestService.createForBatch(harvest);
        s.setCurrentCount(s.getCurrentCount()-request.count());s.setHarvestedCount(s.getHarvestedCount()+request.count());s.setHarvestedCost(s.getHarvestedCost().add(carry));closeIfEmpty(s,request.date());
        BatchMovement event=movement(s,request.requestId(),hash,"HARVEST",request.date(),request.count(),request.notes());event.setAllocatedCost(carry);event.setRevenue(request.revenue());event.setWeightKg(request.weightKg());event.setHarvestId(saved.getId());movements.saveAndFlush(event);
        audit.record("HARVEST","BATCH_STAGE",id,"Harvested "+request.count()+" fish, "+request.weightKg()+" kg");return details(s.getBatch().getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String,Object> settlement(Long id,BatchSettlementRequest request) {
        BatchStage s=locked(id);String hash=fingerprint("SETTLEMENT",id,request);var replay=replay(request.requestId(),hash);if(replay!=null)return replay;
        date(s,request.date());BatchMovement event=movement(s,request.requestId(),hash,"SETTLEMENT",request.date(),0,request.notes());event.setRevenue(request.amount());movements.saveAndFlush(event);
        audit.record("SETTLEMENT","BATCH_STAGE",id,"Additional revenue "+request.amount());return details(s.getBatch().getId());
    }
    private void feedMutable(StageExpense expense) {
        if(!movements.findByStageIdAndKindInAndEventDateGreaterThanEqual(expense.getStage().getId(),List.of("TRANSFER","HARVEST"),expense.getExpenseDate()).isEmpty())
            throw bad("This feeding cost was carried into a transfer or harvest and cannot be changed");
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void syncFeed(DailyFeed feed) {
        StageExpense existing=expenses.findByFeedEntryId(feed.getId()).orElse(null);
        if(existing!=null) {
            feedMutable(existing);BatchStage s=existing.getStage();
            if(!s.getPond().getId().equals(feed.getPond().getId()))throw bad("Undo this batch feeding before moving it to another pond");
            open(s);date(s,feed.getFeedingDate());existing.setAmount(feed.getExpense());existing.setExpenseDate(feed.getFeedingDate());s.setLastActivityOn(feed.getFeedingDate());return;
        }
        BatchStage s=stages.findByPondIdAndCurrentCountGreaterThan(feed.getPond().getId(),0).orElse(null);
        if(s==null || feed.getFeedingDate().isBefore(s.getStartedOn()))return;
        date(s,feed.getFeedingDate());addExpense(s,feed.getFeedingDate(),"FEED",feed.getExpense(),"Daily feeding "+feed.getFeedingTime(),feed.getId());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void removeFeed(DailyFeed feed) {
        var expense=expenses.findByFeedEntryId(feed.getId());if(expense.isPresent()){feedMutable(expense.get());expenses.delete(expense.get());expenses.flush();}
    }
}
