package io.github.ally412.shelter.common.web;

import io.github.ally412.shelter.animal.Status;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        // Invalid Status enum value (thrown in @JsonCreator, wrapped by Jackson) → the specific response.
        if (ex.getMostSpecificCause() instanceof InvalidStatusException ise) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setTitle("Invalid enum value");
            pd.setDetail(ise.getMessage());
            pd.setProperty("allowed (case insensitive)", Status.values());
            return pd;
        }
        // Anything else unreadable (missing body, malformed JSON, ...) → generic, no enum blame.
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Malformed request");
        pd.setDetail("Request body is missing or not readable");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage(),
                        (first, second) -> first,   // keep first message if a field has multiple violations
                        LinkedHashMap::new));

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(DuplicateCredException.class)
    public ProblemDetail handleDuplicateCredException(DuplicateCredException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate Cred");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // DB-level safety net: the UNIQUE constraint firing (e.g. two concurrent registrations
    // that both passed the pre-check). Generic detail on purpose — never leak the raw SQL.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate Cred");
        pd.setDetail("A unique field is already in use");
        return pd;
    }

    // Failed login (bad password / unknown user). Generic detail on purpose:
    // don't reveal WHICH was wrong (prevents username enumeration).
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Authentication failed");
        pd.setDetail("Invalid username or password");
        return pd;
    }



























}
