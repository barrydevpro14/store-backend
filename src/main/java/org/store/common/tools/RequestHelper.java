package org.store.common.tools;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Utilities for extracting HTTP request metadata (IP, User-Agent) within the request thread. */
public final class RequestHelper {

    private RequestHelper() {}

    /** Returns the caller IP, honoring X-Forwarded-For for reverse-proxy setups. */
    public static String getClientIp() {
        try {
            HttpServletRequest req = currentRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Returns the User-Agent header value, or "unknown" if not available. */
    public static String getUserAgent() {
        try {
            String ua = currentRequest().getHeader("User-Agent");
            return ua != null ? ua : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
