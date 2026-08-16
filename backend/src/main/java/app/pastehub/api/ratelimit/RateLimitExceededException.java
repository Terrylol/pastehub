package app.pastehub.api.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfter;

    RateLimitExceededException(long retryAfter) { this.retryAfter = retryAfter; }
    public long retryAfter() { return retryAfter; }
}
