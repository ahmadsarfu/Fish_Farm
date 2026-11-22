package com.fishfarm.management.medicine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static com.fishfarm.management.medicine.MedicineRequests.*;
@RestController @RequestMapping("/api/medicines") @RequiredArgsConstructor
public class MedicineController {
 private final MedicineService service;
 @GetMapping("/products") public Object products(){return service.products();}
 @PostMapping("/products") public Object create(@Valid @RequestBody Product body){return service.saveProduct(null,body);}
 @PutMapping("/products/{id}") public Object update(@PathVariable Long id,@Valid @RequestBody Product body){return service.saveProduct(id,body);}
 @GetMapping("/lots") public Object lots(@RequestParam(required=false) Long productId){return service.lots(productId);}
 @PostMapping("/purchases") public Object purchase(@Valid @RequestBody Purchase body){return service.purchase(body);}
 @PostMapping("/movements") public Object move(@Valid @RequestBody Movement body){return service.move(body);}
 @PostMapping("/movements/{id}/reverse") public Object reverse(@PathVariable Long id,@Valid @RequestBody Reverse body){return service.reverse(id,body);}
 @GetMapping("/history") public Object history(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long productId,@RequestParam(defaultValue="") String kind,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.history(from,to,productId,kind,page,size);}
 @GetMapping("/summary") public Object summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long productId){return service.summary(from,to,productId);}
 @GetMapping("/export") public Object export(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long productId){return service.export(from,to,productId);}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","This entry conflicts with an existing medicine record. Refresh history before retrying."));}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Medicine request failed")));}
}
