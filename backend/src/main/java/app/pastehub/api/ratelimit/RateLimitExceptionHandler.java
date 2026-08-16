package app.pastehub.api.ratelimit;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class RateLimitExceptionHandler {
 @ExceptionHandler(RateLimitExceededException.class) ResponseEntity<Map<String,String>> exceeded(RateLimitExceededException e) { return ResponseEntity.status(429).header("Retry-After", String.valueOf(e.retryAfter())).body(Map.of("code","RATE_LIMITED")); }
 @ExceptionHandler(RateLimitUnavailableException.class) ResponseEntity<Map<String,String>> unavailable() { return ResponseEntity.status(503).body(Map.of("code","RATE_LIMIT_UNAVAILABLE")); }
}
