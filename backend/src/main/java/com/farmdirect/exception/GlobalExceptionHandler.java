package com.farmdirect.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private ResponseEntity<Map<String, Object>> body(HttpStatus s, String m, Object details) {
    Map<String, Object> b = new LinkedHashMap<>();
    b.put("status", s.value()); b.put("message", m);
    if (details != null) b.put("details", details);
    return ResponseEntity.status(s).body(b);
  }
  @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e) { return body(e.getStatus(), e.getMessage(), null); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
    Map<String, String> f = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors().forEach(x -> f.putIfAbsent(x.getField(), x.getDefaultMessage()));
    return body(HttpStatus.BAD_REQUEST, "Validation failed", f);
  }
  @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<?> cv(ConstraintViolationException e) { return body(HttpStatus.BAD_REQUEST, e.getMessage(), null); }
  @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> denied(AccessDeniedException e) { return body(HttpStatus.FORBIDDEN, "Access denied", null); }
  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class) ResponseEntity<?> lock(Exception e) {
    return body(HttpStatus.CONFLICT, "Stock changed while processing. Please retry.", null);
  }
  @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e) {
    log.error("Unhandled error", e);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", null);
  }
}
