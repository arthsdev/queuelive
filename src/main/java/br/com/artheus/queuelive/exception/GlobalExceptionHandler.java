package br.com.artheus.queuelive.exception;

import br.com.artheus.queuelive.exception.base.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(
            BaseException ex, Locale locale) {

        String message = messageSource.getMessage(
                ex.getApiError().getMessageKey(), null, locale);

        return ResponseEntity.status(ex.getStatus()).body(buildBody(
                ex.getStatus(),
                ex.getApiError().name(),
                message
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, Locale locale) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildBody(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                errors.toString()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Locale locale) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildBody(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this action"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred"
        ));
    }

    private Map<String, Object> buildBody(HttpStatus status, String error, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", error,
                "message", message
        );
    }
}