package org.isaac.rest.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.isaac.dto.ErrorResponse;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Exception mapper for REST endpoints that handles validation errors
 * and other common exceptions, providing consistent error responses.
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(RestExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOG.debugf("REST: Validation error occurred - %s", exception.getMessage());

        List<String> validationErrors = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                "Validation failed",
                "The request contains invalid data",
                validationErrors);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}