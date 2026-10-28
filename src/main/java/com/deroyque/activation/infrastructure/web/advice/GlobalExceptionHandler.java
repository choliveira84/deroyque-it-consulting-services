package com.deroyque.activation.infrastructure.web.advice;

import com.deroyque.activation.domain.exception.ActivationConflictException;
import com.deroyque.activation.domain.exception.ActivationNotFoundException;
import com.deroyque.activation.domain.exception.InvalidStateTransitionException;
import com.deroyque.activation.infrastructure.web.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI VALIDATION_TYPE = URI.create("https://api.deroyque.com/problems/validation-error");
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.deroyque.com/problems/resource-not-found");
    private static final URI CONFLICT_TYPE = URI.create("https://api.deroyque.com/problems/conflict");
    private static final URI STATE_ERROR_TYPE = URI.create("https://api.deroyque.com/problems/invalid-state-transition");
    private static final URI FORBIDDEN_TYPE = URI.create("https://api.deroyque.com/problems/access-denied");
    private static final URI UNAUTHORIZED_TYPE = URI.create("https://api.deroyque.com/problems/unauthorized");
    private static final URI INTERNAL_ERROR_TYPE = URI.create("https://api.deroyque.com/problems/internal-error");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error on request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more validation constraints were violated.");
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid Request Data");

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();
        problem.setProperty("errors", fieldErrors);
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Bad Request");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(org.springframework.data.mapping.PropertyReferenceException ex) {
        log.warn("Invalid sort property: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid sort property: '" + ex.getPropertyName() + "'. For sorting, use query format: 'sort=property,asc' or 'sort=property,desc'."
        );
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid Sort Property");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(ActivationNotFoundException.class)
    public ProblemDetail handleNotFound(ActivationNotFoundException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Activation Not Found");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(ActivationConflictException.class)
    public ProblemDetail handleConflict(ActivationConflictException ex) {
        log.warn("Activation conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(CONFLICT_TYPE);
        problem.setTitle("Request Conflict");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidState(InvalidStateTransitionException ex) {
        log.warn("Invalid state transition attempted: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(STATE_ERROR_TYPE);
        problem.setTitle("Invalid State Transition");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Insufficient permissions to perform this action.");
        problem.setType(FORBIDDEN_TYPE);
        problem.setTitle("Access Denied");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Valid JWT Bearer token required.");
        problem.setType(UNAUTHORIZED_TYPE);
        problem.setTitle("Unauthorized");
        enrichTraceId(problem);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled internal exception occurred", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support with the trace identifier."
        );
        problem.setType(INTERNAL_ERROR_TYPE);
        problem.setTitle("Internal Server Error");
        enrichTraceId(problem);
        return problem;
    }

    private void enrichTraceId(ProblemDetail problem) {
        String traceId = MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid"
        );
    }
}
