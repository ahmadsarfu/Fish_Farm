package com.fishfarm.management.medicine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
public interface MedicineMovementRepository extends JpaRepository<MedicineMovement,Long> {
 Optional<MedicineMovement> findByRequestKey(String key);
 @Query("select m.reference.id from MedicineMovement m where m.kind='REVERSAL' and m.reference.id in :ids") List<Long> reversedIds(@Param("ids") List<Long> ids);
 boolean existsByReferenceIdAndKind(Long id,String kind);
 @Query("select coalesce(sum(m.delta),0) from MedicineMovement m where m.lot.id=:id") long stock(@Param("id") Long id);
 @Query("select m.lot.id,sum(m.delta) from MedicineMovement m group by m.lot.id") List<Object[]> balances();
 @Query("select max(m.eventDate) from MedicineMovement m where m.lot.id=:id") LocalDate latestDate(@Param("id") Long id);
 @Query("select coalesce(sum(m.delta),0) from MedicineMovement m left join m.reference r left join r.reference sale where (m.kind='SALES_RETURN' and r.id=:id) or (m.kind='REVERSAL' and r.kind='SALES_RETURN' and sale.id=:id)") long returned(@Param("id") Long id);
 @Query("select count(m) from MedicineMovement m where m.lot.id=:id") long countForLot(@Param("id") Long id);
 @Query("select m from MedicineMovement m join fetch m.lot l join fetch l.product left join fetch m.reference where m.eventDate between :from and :to and (:product is null or l.product.id=:product) and (:kind='' or m.kind=:kind) order by m.eventDate desc,m.id desc")
 List<MedicineMovement> history(@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("product") Long product,@Param("kind") String kind,Pageable page);
 @Query("select count(m) from MedicineMovement m where m.eventDate between :from and :to and (:product is null or m.lot.product.id=:product) and (:kind='' or m.kind=:kind)")
 long historyCount(@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("product") Long product,@Param("kind") String kind);
 @Query("select m.kind,r.kind,sum(m.delta*m.unitPrice),sum(m.delta*l.unitCost) from MedicineMovement m join m.lot l left join m.reference r where m.eventDate between :from and :to and (:product is null or l.product.id=:product) group by m.kind,r.kind")
 List<Object[]> totals(@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("product") Long product);
}
