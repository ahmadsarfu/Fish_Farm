package com.fishfarm.management.medicine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface MedicineLotRepository extends JpaRepository<MedicineLot,Long> {
 @Query("select l from MedicineLot l join fetch l.product p order by l.expiryDate,l.id") List<MedicineLot> allWithProducts();
 boolean existsByProductId(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from MedicineLot l where l.id=:id") Optional<MedicineLot> locked(@Param("id") Long id);
}
