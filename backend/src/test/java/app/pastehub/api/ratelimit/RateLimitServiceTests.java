package app.pastehub.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;

class RateLimitServiceTests {
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> operations = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @Test
    void permitsRequestsUntilConfiguredLimitThenRejectsWithoutCallingBusinessCode() {
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.increment(anyString())).thenReturn(1L, 2L, 3L);
        RateLimitService service = service(2);

        assertDoesNotThrow(() -> service.check("text-create", "203.0.113.1"));
        assertDoesNotThrow(() -> service.check("text-create", "203.0.113.1"));
        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> service.check("text-create", "203.0.113.1"));
        verify(redis).expire(anyString(), any(Duration.class));
        org.junit.jupiter.api.Assertions.assertTrue(exception.retryAfter() > 0);
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(RateLimitUnavailableException.class, () -> service(10).check("text-create", "203.0.113.1"));
    }

    private RateLimitService service(int textLimit) {
        return new RateLimitService(redis, true, 60, textLimit, 10, 20, 20);
    }
}
