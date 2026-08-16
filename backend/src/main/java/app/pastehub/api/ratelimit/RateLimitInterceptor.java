package app.pastehub.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService service;
    private final String trustedProxies;
    public RateLimitInterceptor(RateLimitService service, @Value("${pastehub.rate-limit.trusted-proxies:}") String trustedProxies) { this.service = service; this.trustedProxies = trustedProxies; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI(), method = request.getMethod();
        String action = action(path, method); if (action != null) service.check(action, clientIp(request)); return true;
    }
    private String action(String path, String method) {
        if ("POST".equals(method) && path.endsWith("/text")) return "text-create";
        if ("POST".equals(method) && path.endsWith("/image/init")) return "image-init";
        if ("POST".equals(method) && path.endsWith("/image/complete")) return "image-complete";
        if ("DELETE".equals(method) && path.startsWith("/api/v1/transfers/")) return "delete";
        return null;
    }
    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!trustedProxies.isBlank() && java.util.Arrays.stream(trustedProxies.split(",")).map(String::trim).anyMatch(remote::equals)) {
            String forwarded = request.getHeader("X-Forwarded-For"); if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        }
        return remote;
    }
}
