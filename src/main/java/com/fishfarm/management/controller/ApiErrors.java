package com.fishfarm.management.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Request failed"))); }
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).reduce((a,b)->a+"; "+b).orElse("Invalid request")));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> integrity() { return ResponseEntity.status(409).body(Map.of("message","A matching record already exists, or this record is still in use")); }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> malformed(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Invalid request body or parameter")); }
    @ExceptionHandler({jakarta.validation.ConstraintViolationException.class,org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<?> parameters(Exception e) {return ResponseEntity.badRequest().body(Map.of("message","One or more request parameters are invalid"));}
    @ExceptionHandler(RuntimeException.class) public ResponseEntity<?> legacy(RuntimeException e) {
        String message=Objects.requireNonNullElse(e.getMessage(),"");
        if(message.toLowerCase(Locale.ROOT).endsWith("not found"))return ResponseEntity.status(404).body(Map.of("message",message));
        if(message.toLowerCase(Locale.ROOT).contains("already exists"))return ResponseEntity.status(409).body(Map.of("message",message));
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("API operation failed",e);
        return ResponseEntity.status(500).body(Map.of("message","Unable to complete the request. Check the server logs."));
    }
}
