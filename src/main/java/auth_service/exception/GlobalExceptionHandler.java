package auth_service.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.security.auth.message.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException ex) {
        return new ResponseEntity<>("Invalid credentials, please try again.",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<AppError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        AppError appError = new AppError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(appError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AppError> handleAuthException(AuthException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AppError> handleIllegalArgumentException(IllegalArgumentException ex) {
        AppError appError = new AppError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(appError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppError> handleGenericException(Exception ex) {
        AppError appError = new AppError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later."
        );
        return new ResponseEntity<>(appError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<AppError> handleExpiredJwtException(ExpiredJwtException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), "Token expired");
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<AppError> handleSignatureException(SignatureException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), "Invalid token signature");
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<AppError> handleMalformedJwtException(MalformedJwtException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), "Malformed token");
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<AppError> handleDisabledException(DisabledException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), "Account is disabled");
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<AppError> handleLockedException(LockedException ex) {
        AppError appError = new AppError(HttpStatus.UNAUTHORIZED.value(), "Account is locked");
        return new ResponseEntity<>(appError, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AppError> handleRuntimeException(RuntimeException ex) {
        AppError appError = new AppError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "A runtime error occurred: " + ex.getMessage()
        );
        return new ResponseEntity<>(appError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}