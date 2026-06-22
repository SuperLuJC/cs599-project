package com.ccsurvey.modules.auth.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.util.JwtUtils;
import com.ccsurvey.modules.auth.dto.LoginRequest;
import com.ccsurvey.modules.auth.dto.LoginResponse;
import com.ccsurvey.modules.auth.dto.RegisterRequest;
import com.ccsurvey.modules.auth.service.AuthService;
import com.ccsurvey.modules.log.annotation.OperLog;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @Value("${jwt.cookie-name:cc_survey_token}")
    private String cookieName;

    @Value("${jwt.cookie-max-age:7200}")
    private int cookieMaxAge;

    @Value("${jwt.expiration:7200000}")
    private long jwtExpiration;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @OperLog(title = "用户登录", businessType = "LOGIN")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);

        // 生成JWT Token
        String token = jwtUtils.generateToken(loginResponse.getUserId(), loginResponse.getUsername(), loginResponse.getName(), loginResponse.getRole());

        // 设置HttpOnly Cookie
        setTokenCookie(response, token);

        // 返回用户信息 (不返回token，token在Cookie中)
        return ApiResponse.success("登录成功", loginResponse);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @OperLog(title = "用户注册", businessType = "REGISTER")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功，请查收验证邮件");
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @OperLog(title = "用户登出", businessType = "LOGOUT")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 从Cookie中获取token
        String token = getTokenFromCookie(request);

        if (token != null && jwtUtils.validateToken(token)) {
            String userId = jwtUtils.getUserId(token);
            authService.logout(userId, token);
        }

        // 清除Cookie
        clearTokenCookie(response);

        return ApiResponse.success("登出成功");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refreshToken(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ApiResponse.error(ErrorCode.TOKEN_MISSING);
        }

        String newToken = authService.refreshToken(refreshToken);

        // 设置新Cookie
        setTokenCookie(response, newToken);

        Map<String, String> result = new HashMap<>();
        result.put("token", newToken);
        return ApiResponse.success(result);
    }

    /**
     * 验证邮箱
     */
    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.success("邮箱验证成功");
    }

    /**
     * 请求密码重置
     */
    @PostMapping("/password/reset-request")
    public ApiResponse<Void> requestPasswordReset(
            @RequestParam @Email(message = "邮箱格式不正确") String email) {
        authService.requestPasswordReset(email);
        return ApiResponse.success("如果邮箱存在，重置邮件已发送");
    }

    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(
            @RequestParam String token,
            @RequestParam @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间") String newPassword,
            HttpServletResponse response) {

        authService.resetPassword(token, newPassword);

        // 清除Cookie，强制重新登录
        clearTokenCookie(response);

        return ApiResponse.success("密码重置成功，请重新登录");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<LoginResponse> getCurrentUser(HttpServletRequest request) {
        String token = getTokenFromCookie(request);

        if (token == null || !jwtUtils.validateToken(token)) {
            return ApiResponse.error(ErrorCode.UNAUTHORIZED);
        }

        // 从token获取用户信息
        String userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        String name = jwtUtils.getName(token);
        String role = jwtUtils.getRole(token);

        LoginResponse userInfo = LoginResponse.builder()
                .userId(userId)
                .uuid(userId)
                .username(username)
                .name(name)
                .role(role)
                .build();

        return ApiResponse.success(userInfo);
    }

    // ==================== 私有方法 ====================

    /**
     * 设置Token Cookie (HttpOnly)
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 开发环境设为false，生产环境应为true
        cookie.setPath("/");
        cookie.setMaxAge(cookieMaxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    /**
     * 清除Token Cookie
     */
    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * 从Cookie获取Token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}