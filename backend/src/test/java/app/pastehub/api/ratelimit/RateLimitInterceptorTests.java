package app.pastehub.api.ratelimit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitInterceptorTests {
    @Test
    void ignoresForwardedHeaderFromUntrustedRemoteAddress() {
        RateLimitService service = mock(RateLimitService.class);
        MockHttpServletRequest request = request("10.0.0.8", "198.51.100.2");
        new RateLimitInterceptor(service, "127.0.0.1").preHandle(request, null, new Object());
        verify(service).check(eq("text-create"), eq("10.0.0.8"));
    }

    @Test
    void acceptsFirstForwardedAddressOnlyFromTrustedProxy() {
        RateLimitService service = mock(RateLimitService.class);
        MockHttpServletRequest request = request("127.0.0.1", "198.51.100.2, 10.0.0.8");
        new RateLimitInterceptor(service, "127.0.0.1").preHandle(request, null, new Object());
        verify(service).check(eq("text-create"), eq("198.51.100.2"));
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers/text");
        request.setRemoteAddr(remoteAddress); request.addHeader("X-Forwarded-For", forwardedFor); return request;
    }
}
