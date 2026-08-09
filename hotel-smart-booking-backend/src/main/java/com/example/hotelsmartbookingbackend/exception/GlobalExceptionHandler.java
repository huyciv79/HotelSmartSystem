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

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> handleForbiddenException(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (detail != null) {
            String lowerDetail = detail.toLowerCase();
            if (lowerDetail.contains("users_email_key") || lowerDetail.contains("email")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email đã tồn tại trong hệ thống"));
            }
            if (lowerDetail.contains("users_phonenumber_key") || lowerDetail.contains("phonenumber")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại này đã tồn tại trong hệ thống"));
            }
            if (lowerDetail.contains("users_idcardnumber_key") || lowerDetail.contains("idcardnumber")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số CCCD này đã tồn tại trong hệ thống"));
            }
            if (lowerDetail.contains("users_idcardnumber_check")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số CCCD phải gồm đúng 12 chữ số"));
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu không hợp lệ hoặc bị trùng lặp"));
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
