package com.example.serach.location.search_location.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class FingerprintUtil {

    public String createConsistentFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(getHeaderSafe(request, "User-Agent")).append("|");
        fingerprint.append(getHeaderSafe(request, "Accept-Language")).append("|");
        fingerprint.append(getHeaderSafe(request, "Sec-Ch-Ua")).append("|");
        fingerprint.append(getHeaderSafe(request, "Sec-Ch-Ua-Platform")).append("|");
        fingerprint.append(getHeaderSafe(request, "Sec-Ch-Ua-Mobile")).append("|");
        fingerprint.append(getClientIp(request));
        return fingerprint.toString();
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getHeaderSafe(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value != null ? value : "";
    }
}