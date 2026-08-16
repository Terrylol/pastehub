package app.pastehub.api.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final long windowSeconds;
    private final Map<String, Integer> limits;

    public RateLimitService(StringRedisTemplate redis,
            @Value("${pastehub.rate-limit.enabled:false}") boolean enabled,
            @Value("${pastehub.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${pastehub.rate-limit.text-create-limit:10}") int textCreateLimit,
            @Value("${pastehub.rate-limit.image-init-limit:10}") int imageInitLimit,
            @Value("${pastehub.rate-limit.image-complete-limit:20}") int imageCompleteLimit,
            @Value("${pastehub.rate-limit.delete-limit:20}") int deleteLimit) {
        this.redis = redis; this.enabled = enabled; this.windowSeconds = windowSeconds;
        limits = Map.of("text-create", textCreateLimit, "image-init", imageInitLimit,
                "image-complete", imageCompleteLimit, "delete", deleteLimit);
    }

    public void check(String action, String clientIp) {
        if (!enabled) return;
        long window = Instant.now().getEpochSecond() / windowSeconds;
        long retryAfter = windowSeconds - Instant.now().getEpochSecond() % windowSeconds;
        try {
            Long count = redis.opsForValue().increment("pastehub:rate-limit:" + action + ":" + clientIp + ":" + window);
            if (count != null && count == 1) redis.expire("pastehub:rate-limit:" + action + ":" + clientIp + ":" + window, Duration.ofSeconds(retryAfter));
            if (count == null) throw new RateLimitUnavailableException();
            if (count > limits.get(action)) throw new RateLimitExceededException(retryAfter);
        } catch (RateLimitExceededException exception) { throw exception;
        } catch (Exception exception) { throw new RateLimitUnavailableException(); }
    }
}
