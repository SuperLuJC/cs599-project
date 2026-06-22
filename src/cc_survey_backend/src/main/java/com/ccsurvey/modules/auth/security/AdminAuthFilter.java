package com.ccsurvey.modules.auth.security;

import com.ccsurvey.common.util.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 管理员权限过滤器
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class AdminAuthFilter implements Filter {

    private final JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 只检查 /api/admin/** 路径
        if (!path.startsWith("/api/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 userId 和 role（由 JwtAuthenticationFilter 设置）
        String userId = (String) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");

        if (userId == null) {
            // 未登录，由 JwtAuthenticationFilter 处理
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是管理员
        if (!"admin".equals(role)) {
            log.warn("非管理员尝试访问管理接口: userId={}, role={}, path={}", userId, role, path);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }
}