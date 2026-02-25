package com.historyai.controller;

import com.historyai.dto.ErrorResponse;
import com.historyai.exception.CharacterAlreadyExistsException;
import com.historyai.exception.CharacterNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/characters");
    }

    @Test
    void handleCharacterNotFound_ShouldReturn404() {
        UUID characterId = UUID.randomUUID();
        CharacterNotFoundException ex = new CharacterNotFoundException(characterId);
        when(request.getMethod()).thenReturn("GET");

        ResponseEntity<ErrorResponse> response = handler.handleCharacterNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains(characterId.toString()));
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void handleCharacterAlreadyExists_ShouldReturn409() {
        String characterName = "Mikołaj Kopernik";
        CharacterAlreadyExistsException ex = new CharacterAlreadyExistsException(characterName);
        when(request.getMethod()).thenReturn("POST");

        ResponseEntity<ErrorResponse> response = handler.handleCharacterAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains(characterName));
    }

    @Test
    void handleIllegalArgument_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        when(request.getMethod()).thenReturn("POST");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void handleNoSuchElement_ShouldReturn404() {
        NoSuchElementException ex = new NoSuchElementException("Element not found");
        when(request.getMethod()).thenReturn("GET");

        ResponseEntity<ErrorResponse> response = handler.handleNoSuchElement(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
    }

    @Test
    void handleTypeMismatch_ShouldReturn400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalid", UUID.class, "id", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("UUID"));
    }

    @Test
    void handleGenericException_ShouldReturn500() {
        Exception ex = new RuntimeException("Unexpected error");
        when(request.getMethod()).thenReturn("GET");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
    }

    @Test
    void handleValidationErrors_ShouldReturn400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        var fieldError = new org.springframework.validation.FieldError("name", "name", "must not be blank");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertNotNull(response.getBody().getFieldErrors());
        assertEquals(1, response.getBody().getFieldErrors().size());
    }

    @Test
    void errorResponse_ShouldContainTraceId() {
        CharacterNotFoundException ex = new CharacterNotFoundException(UUID.randomUUID());
        when(request.getMethod()).thenReturn("GET");

        ResponseEntity<ErrorResponse> response = handler.handleCharacterNotFound(ex, request);

        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void errorResponse_ShouldContainTimestamp() {
        CharacterNotFoundException ex = new CharacterNotFoundException(UUID.randomUUID());
        when(request.getMethod()).thenReturn("GET");

        ResponseEntity<ErrorResponse> response = handler.handleCharacterNotFound(ex, request);

        assertNotNull(response.getBody().getTimestamp());
    }
}
