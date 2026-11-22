package com.fishfarm.management.medicine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishfarm.management.security.FarmContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import static com.fishfarm.management.medicine.MedicineRequests.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class MedicineService {
 private final MedicineProductRepository products;
 private final MedicineLotRepository lots;
 private final MedicineMovementRepository movements;
 private final ObjectMapper json;
 private final Clock clock;
 private LocalDate today(){return LocalDate.now(clock);}
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
 private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine record not found");}
 private String clean(String value){return value==null?"":value.strip();}
 private Map<String,Object> row(Object... values){var map=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)map.put((String)values[i],values[i+1]);return map;}
 private byte[] fingerprint(Object body){try{return MessageDigest.getInstance("SHA-256").digest((body.getClass().getName()+json.writeValueAsString(body)).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 private MedicineMovement replay(UUID key,Object body){var saved=movements.findByRequestKey(key.toString());if(saved.isEmpty())return null;if(!MessageDigest.isEqual(saved.get().getPayloadHash(),fingerprint(body)))throw new ResponseStatusException(HttpStatus.CONFLICT,"This request key was already used for different data. Refresh history.");return saved.get();}
 private Map<Long,Long> balances(){var result=new HashMap<Long,Long>();for(var r:movements.balances())result.put(((Number)r[0]).longValue(),((Number)r[1]).longValue());return result;}
 private MedicineProduct product(Long id){return products.locked(id).orElseThrow(this::missing);}
 private MedicineLot lot(Long id){return lots.locked(id).orElseThrow(this::missing);}
 private void chronological(MedicineLot lot,LocalDate date){LocalDate last=movements.latestDate(lot.getId());if(date.isBefore(lot.getReceivedOn()) || (last!=null && date.isBefore(last)))throw bad("Use a date on or after this lot's latest movement: "+(last==null?lot.getReceivedOn():last));}
 public List<Map<String,Object>> products(){
  var totals=balances();var stock=new HashMap<Long,Long>();var expired=new HashMap<Long,Long>();
  for(var l:lots.allWithProducts()){long n=totals.getOrDefault(l.getId(),0L);stock.merge(l.getProduct().getId(),n,Long::sum);if(l.getExpiryDate().isBefore(today()))expired.merge(l.getProduct().getId(),n,Long::sum);}
  return products.findAllByOrderByNameAsc().stream().map(p->row("id",p.getId(),"name",p.getName(),"brand",p.getBrand(),"packLabel",p.getPackLabel(),"unit",p.getUnit(),"minimumStock",p.getMinimumStock(),"sellingPrice",p.getSellingPrice(),"active",p.isActive(),"version",p.getVersion(),"identityLocked",stock.containsKey(p.getId()),"stock",stock.getOrDefault(p.getId(),0L),"expiredStock",expired.getOrDefault(p.getId(),0L))).toList();
 }
 public List<Map<String,Object>> lots(Long productId){var balances=balances();return lots.allWithProducts().stream().filter(l->productId==null||l.getProduct().getId().equals(productId)).map(l->row("id",l.getId(),"productId",l.getProduct().getId(),"medicine",l.getProduct().getName(),"packLabel",l.getProduct().getPackLabel(),"unit",l.getProduct().getUnit(),"active",l.getProduct().isActive(),"sellingPrice",l.getProduct().getSellingPrice(),"batchNumber",l.getBatchNumber(),"expiryDate",l.getExpiryDate(),"receivedOn",l.getReceivedOn(),"unitCost",l.getUnitCost(),"stock",balances.getOrDefault(l.getId(),0L),"expired",l.getExpiryDate().isBefore(today()),"expiringSoon",!l.getExpiryDate().isBefore(today())&&!l.getExpiryDate().isAfter(today().plusDays(30)))).toList();}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Map<String,Object> saveProduct(Long id,Product body){
  MedicineProduct p=id==null?new MedicineProduct():product(id);
  if(id!=null && (body.expectedVersion()==null || body.expectedVersion()!=p.getVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Product changed. Refresh before editing.");
  String name=clean(body.name()),brand=clean(body.brand()),pack=clean(body.packLabel());
  if(products.existsByNameAndBrandAndPackLabelAndIdNot(name,brand,pack,id==null?-1L:id))throw new ResponseStatusException(HttpStatus.CONFLICT,"This medicine and pack already exist.");
  if(id!=null && lots.existsByProductId(id) && (!p.getName().equals(name)||!p.getBrand().equals(brand)||!p.getPackLabel().equals(pack)||!p.getUnit().equals(body.unit().name())))throw bad("Medicine identity and pack are locked after the first purchase. Add a separate product for a different pack.");
  p.setName(name);p.setBrand(brand);p.setPackLabel(pack);p.setUnit(body.unit().name());p.setMinimumStock(body.minimumStock());p.setSellingPrice(body.sellingPrice());p.setActive(body.active());products.saveAndFlush(p);return row("id",p.getId(),"version",p.getVersion());
 }
 private MedicineMovement append(MedicineLot lot,String kind,LocalDate date,int delta,BigDecimal price,String party,String notes,MedicineMovement reference,UUID key,Object payload){
  long current=movements.stock(lot.getId());if(current+delta<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Not enough stock. Available: "+current);
  var m=new MedicineMovement();m.setLot(lot);m.setKind(kind);m.setEventDate(date);m.setDelta(delta);m.setUnitPrice(price);m.setParty(clean(party));m.setNotes(clean(notes));m.setReference(reference);m.setRequestKey(key.toString());m.setPayloadHash(fingerprint(payload));return movements.saveAndFlush(m);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Map<String,Object> purchase(Purchase body){
  var replay=replay(body.requestKey(),body);if(replay!=null)return movementRow(replay);
  var p=product(body.productId());replay=replay(body.requestKey(),body);if(replay!=null)return movementRow(replay);
  if(!p.isActive())throw bad("Choose an active medicine");if(body.expiryDate().isBefore(body.date()))throw bad("Expiry cannot be before the receipt date");
  var lot=new MedicineLot();lot.setProduct(p);lot.setBatchNumber(clean(body.batchNumber()));lot.setReceivedOn(body.date());lot.setExpiryDate(body.expiryDate());lot.setUnitCost(body.unitCost());lots.saveAndFlush(lot);
  return movementRow(append(lot,"PURCHASE",body.date(),body.quantity(),body.unitCost(),body.supplier(),body.notes(),null,body.requestKey(),body));
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Map<String,Object> move(Movement body){
  var l=lot(body.lotId());var replay=replay(body.requestKey(),body);if(replay!=null)return movementRow(replay);chronological(l,body.date());
  String kind=body.kind().name();MedicineMovement reference=null;BigDecimal price=l.getUnitCost();int delta=-body.quantity();
  if(body.kind()==Kind.SALE || body.kind()==Kind.OWN_USE){if(!l.getProduct().isActive())throw bad("This medicine is inactive");if(l.getExpiryDate().isBefore(body.date()))throw bad("Expired medicine cannot be sold or used");}
  if(body.kind()==Kind.SALE){if(body.unitPrice()==null)throw bad("Enter the final selling price per bottle/packet");price=body.unitPrice();}
  if(body.kind()==Kind.OWN_USE || body.kind()==Kind.DAMAGE){if(clean(body.notes()).length()<5)throw bad("Enter a reason of at least 5 characters");}
  if(body.kind()==Kind.SALES_RETURN){
   if(body.referenceId()==null)throw bad("Select the original sale for this return");reference=movements.findById(body.referenceId()).orElseThrow(this::missing);
   if(!reference.getLot().getId().equals(l.getId()) || !"SALE".equals(reference.getKind()) || movements.existsByReferenceIdAndKind(reference.getId(),"REVERSAL"))throw bad("Choose an active sale from the same medicine lot");
   if(movements.returned(reference.getId())+body.quantity()>-reference.getDelta())throw bad("Return exceeds the quantity sold");
   delta=body.quantity();price=reference.getUnitPrice();
  } else if(body.referenceId()!=null)throw bad("A sale reference is only used for sales returns");
  return movementRow(append(l,kind,body.date(),delta,price,body.party(),body.notes(),reference,body.requestKey(),body));
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Map<String,Object> reverse(Long id,Reverse body){
  if(!"OWNER".equals(FarmContext.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the owner can reverse medicine entries");
  var original=movements.findById(id).orElseThrow(this::missing);var l=lot(original.getLot().getId());var replay=replay(body.requestKey(),body);if(replay!=null){if(!replay.getReference().getId().equals(id))throw bad("Request belongs to another entry");return movementRow(replay);}
  if(clean(body.reason()).length()<5)throw bad("Enter a meaningful reversal reason");
  if("REVERSAL".equals(original.getKind()) || movements.existsByReferenceIdAndKind(id,"REVERSAL"))throw bad("This entry cannot be reversed again");
  if("SALE".equals(original.getKind()) && movements.returned(id)>0)throw bad("Reverse its sales returns before reversing this sale");
  if("PURCHASE".equals(original.getKind()) && movements.countForLot(l.getId())>1)throw bad("This receipt has later movements. Record a supplier return for remaining stock.");
  if("SALES_RETURN".equals(original.getKind()) && movements.existsByReferenceIdAndKind(original.getReference().getId(),"REVERSAL"))throw bad("The original sale was reversed");
  return movementRow(append(l,"REVERSAL",today(),-original.getDelta(),original.getUnitPrice(),original.getParty(),body.reason(),original,body.requestKey(),body));
 }
 private List<Map<String,Object>> movementRows(List<MedicineMovement> entries){
  if(entries.isEmpty())return List.of();
  Set<Long> reversed=new HashSet<>(movements.reversedIds(entries.stream().map(MedicineMovement::getId).toList()));
  return entries.stream().map(m->movementRow(m,reversed.contains(m.getId()))).toList();
 }
 private Map<String,Object> movementRow(MedicineMovement m){return movementRow(m,movements.existsByReferenceIdAndKind(m.getId(),"REVERSAL"));}
 private Map<String,Object> movementRow(MedicineMovement m,boolean reversed){var l=m.getLot();return row("id",m.getId(),"lotId",l.getId(),"productId",l.getProduct().getId(),"medicine",l.getProduct().getName(),"packLabel",l.getProduct().getPackLabel(),"unit",l.getProduct().getUnit(),"batchNumber",l.getBatchNumber(),"expiryDate",l.getExpiryDate(),"kind",m.getKind(),"date",m.getEventDate(),"delta",m.getDelta(),"unitPrice",m.getUnitPrice(),"unitCost",l.getUnitCost(),"amount",m.getUnitPrice().multiply(BigDecimal.valueOf(Math.abs(m.getDelta()))),"party",m.getParty(),"notes",m.getNotes(),"referenceId",m.getReference()==null?null:m.getReference().getId(),"referenceKind",m.getReference()==null?null:m.getReference().getKind(),"recordedBy",m.getRecordedBy(),"recordedAt",m.getRecordedAt(),"reversed",reversed);}
 private void range(LocalDate from,LocalDate to){if(from.isAfter(to))throw bad("From date must be on or before To date");}
 public Map<String,Object> history(LocalDate from,LocalDate to,Long product,String kind,int page,int size){range(from,to);size=Math.max(1,Math.min(size,100));page=Math.max(0,page);return row("items",movementRows(movements.history(from,to,product,kind,PageRequest.of(page,size))),"total",movements.historyCount(from,to,product,kind),"page",page,"size",size);}
 public Map<String,Object> summary(LocalDate from,LocalDate to,Long product){
  range(from,to);BigDecimal sales=BigDecimal.ZERO,salesCost=BigDecimal.ZERO,own=BigDecimal.ZERO,purchases=BigDecimal.ZERO,damage=BigDecimal.ZERO;
  for(var r:movements.totals(from,to,product)){String root="REVERSAL".equals(r[0])?(String)r[1]:(String)r[0];var amount=(BigDecimal)r[2];var cost=(BigDecimal)r[3];
   switch(root){case "SALE","SALES_RETURN"->{sales=sales.subtract(amount);salesCost=salesCost.subtract(cost);}case "OWN_USE"->own=own.subtract(cost);case "PURCHASE","SUPPLIER_RETURN"->purchases=purchases.add(cost);case "DAMAGE"->damage=damage.subtract(cost);default->{}}
  }
  return row("sales",sales,"salesCost",salesCost,"grossProfit",sales.subtract(salesCost),"ownUseCost",own,"purchases",purchases,"damageCost",damage);
 }
 public Map<String,Object> export(LocalDate from,LocalDate to,Long product){range(from,to);if(movements.historyCount(from,to,product,"")>10000)throw bad("Export is limited to 10,000 movements. Choose a shorter date range.");return row("items",movementRows(movements.history(from,to,product,"",PageRequest.of(0,10000))));}
}
