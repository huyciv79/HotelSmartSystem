package com.example.hotelsmartbookingbackend.exception;

import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (detail != null) {
            if (detail.contains("users_idcardnumber_key")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("So CCCD nay da ton tai trong he thong"));
            }
            if (detail.contains("users_idcardnumber_check")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("So CCCD phai gom dung 12 chu so"));
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Du lieu khong hop le hoac bi trung lap"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() : error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<String>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() : error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
