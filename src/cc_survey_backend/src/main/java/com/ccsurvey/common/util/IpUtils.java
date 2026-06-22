package com.ccsurvey.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 */
public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }
        return getClientIp(attributes.getRequest());
    }

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (isEmptyIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况 (X-Forwarded-For可能包含多个IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 处理本地IPv6地址
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IP;
        }

        return ip;
    }

    /**
     * 判断是否为空IP
     */
    private static boolean isEmptyIp(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 获取主机名
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return UNKNOWN;
        }
    }

    /**
     * 获取主机IP
     */
    public static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return LOCALHOST_IP;
        }
    }

    /**
     * 检查是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        byte[] bytes = textToNumericFormatV4(ip);
        if (bytes == null) {
            return false;
        }

        // 10.x.x.x
        if (bytes[0] == (byte) 10) {
            return true;
        }

        // 172.16.x.x - 172.31.x.x
        if (bytes[0] == (byte) 172 && (bytes[1] >= 16 && bytes[1] <= 31)) {
            return true;
        }

        // 192.168.x.x
        if (bytes[0] == (byte) 192 && bytes[1] == (byte) 168) {
            return true;
        }

        // 127.x.x.x
        if (bytes[0] == (byte) 127) {
            return true;
        }

        return false;
    }

    /**
     * 将IP字符串转换为字节数组
     */
    private static byte[] textToNumericFormatV4(String text) {
        byte[] bytes = new byte[4];
        String[] parts = text.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        try {
            for (int i = 0; i < 4; i++) {
                int b = Integer.parseInt(parts[i]);
                if (b < 0 || b > 255) {
                    return null;
                }
                bytes[i] = (byte) b;
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}