package com.buyapp.media.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAppException_notFound_returns404() {
        AppException ex = new AppException("Media not found", HttpStatus.NOT_FOUND);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", 404);
        assertThat(response.getBody()).containsEntry("message", "Media not found");
    }

    @Test
    void handleAppException_badRequest_returns400() {
        AppException ex = new AppException("File too large", HttpStatus.BAD_REQUEST);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", 400);
    }

    @Test
    void handleAccessDenied_rethrowsException() {
        assertThatThrownBy(() -> handler.handleAccessDenied(new AccessDeniedException("forbidden")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void handleMethodNotAllowed_returns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PUT");
        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().get("message").toString()).contains("PUT");
    }

    @Test
    void handleMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body");
        ResponseEntity<Map<String, Object>> response = handler.handleMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("Malformed");
    }

    @Test
    void handleMaxUploadSize_returns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(2 * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("2MB");
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("code", 500);
    }

    @Test
    void responseBody_containsAllRequiredFields() {
        AppException ex = new AppException("test", HttpStatus.CONFLICT);
        ResponseEntity<Map<String, Object>> response = handler.handleAppException(ex);

        assertThat(response.getBody()).containsKeys("code", "message", "details");
    }
}
