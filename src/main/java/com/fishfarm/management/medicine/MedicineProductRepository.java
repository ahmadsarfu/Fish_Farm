package com.fishfarm.management.medicine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface MedicineProductRepository extends JpaRepository<MedicineProduct,Long> {
 List<MedicineProduct> findAllByOrderByNameAsc();
 boolean existsByNameAndBrandAndPackLabelAndIdNot(String name,String brand,String packLabel,Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from MedicineProduct p where p.id=:id") Optional<MedicineProduct> locked(@Param("id") Long id);
}
