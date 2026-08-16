package app.pastehub.api.transfer;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TransferExceptionHandler {

    @ExceptionHandler(TransferUnavailableException.class)
    ResponseEntity<Map<String, Object>> unavailable() {
        return error(HttpStatus.NOT_FOUND, "TRANSFER_UNAVAILABLE");
    }

    @ExceptionHandler(DeleteTokenInvalidException.class)
    ResponseEntity<Map<String, Object>> invalidDeleteToken() {
        return error(HttpStatus.FORBIDDEN, "DELETE_TOKEN_INVALID");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(Map.of("code", code, "timestamp", Instant.now().toString()));
    }
}
