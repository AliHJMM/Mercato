package com.buyapp.media.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(AppException ex) {
        return buildError(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed: " + ex.getMethod(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed or invalid request body", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "File size exceeds the maximum allowed limit of 2MB", null);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Missing required form-data part: " + ex.getRequestPartName(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Missing required request parameter: " + ex.getParameterName(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "Endpoint not found", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
