package com.buyapp.user.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAppException_notFound_returns404() {
        AppException ex = new AppException("Resource not found", HttpStatus.NOT_FOUND);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", 404);
        assertThat(response.getBody()).containsEntry("message", "Resource not found");
    }

    @Test
    void handleAppException_conflict_returns409() {
        AppException ex = new AppException("Email already in use", HttpStatus.CONFLICT);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("code", 409);
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("message").toString()).contains("Access denied");
    }

    @Test
    void handleMethodNotAllowed_returns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().get("message").toString()).contains("DELETE");
    }

    @Test
    void handleMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body");
        ResponseEntity<Map<String, Object>> response = handler.handleMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("Malformed");
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("code", 500);
    }

    @Test
    void responseBody_alwaysContainsCodeMessageDetails() {
        AppException ex = new AppException("test", HttpStatus.BAD_REQUEST);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getBody()).containsKeys("code", "message", "details");
    }
}
