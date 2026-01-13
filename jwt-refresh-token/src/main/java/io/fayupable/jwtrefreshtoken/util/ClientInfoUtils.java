package io.fayupable.jwtrefreshtoken.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Client Information Utility
 * <p>
 * Helper class to extract client details (IP, User-Agent) from the current HTTP request.
 * Removes the need to pass HttpServletRequest or IP strings to service methods.
 * <p>
 * How it works:
 * Uses Spring's RequestContextHolder to access the current thread's HttpServletRequest.
 */
@UtilityClass
public class ClientInfoUtils {

    /**
     * Get Client IP Address
     * <p>
     * Handles proxies and load balancers (X-Forwarded-For).
     *
     * @return IP address string or "0.0.0.0" if unknown
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return "0.0.0.0";

        // Check standard proxy headers
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // If multiple IPs (comma separated), take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Get User Agent (Device Info)
     *
     * @return User-Agent string or "Unknown"
     */
    public static String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return "Unknown";

        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isBlank()) ? userAgent : "Unknown";
    }

    /**
     * Helper to get current HttpServletRequest
     */
    private static HttpServletRequest getCurrentRequest() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }
}