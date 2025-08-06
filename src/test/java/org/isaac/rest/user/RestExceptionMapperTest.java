package org.isaac.rest.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.isaac.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RestExceptionMapper to verify proper handling of validation
 * errors.
 */
class RestExceptionMapperTest {

    private RestExceptionMapper exceptionMapper;

    @BeforeEach
    void setUp() {
        exceptionMapper = new RestExceptionMapper();
    }

    @Test
    void shouldMapConstraintViolationExceptionToBadRequest() {
        // Given
        ConstraintViolation<?> violation1 = Mockito.mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = Mockito.mock(ConstraintViolation.class);

        when(violation1.getMessage()).thenReturn("Name is required");
        when(violation2.getMessage()).thenReturn("Valid email is required");

        Set<ConstraintViolation<?>> violations = Set.of(violation1, violation2);
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        Response response = exceptionMapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("Validation failed", errorResponse.getError());
        assertEquals("The request contains invalid data", errorResponse.getMessage());
        assertNotNull(errorResponse.getDetails());
        assertEquals(2, errorResponse.getDetails().size());
        assertTrue(errorResponse.getDetails().contains("Name is required"));
        assertTrue(errorResponse.getDetails().contains("Valid email is required"));
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void shouldHandleEmptyConstraintViolations() {
        // Given
        Set<ConstraintViolation<?>> violations = Set.of();
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        Response response = exceptionMapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("Validation failed", errorResponse.getError());
        assertEquals("The request contains invalid data", errorResponse.getMessage());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().isEmpty());
    }
}