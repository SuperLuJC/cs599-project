package com.ccsurvey.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:cc-survey}")
    private String issuer;

    @Value("${jwt.expiration:7200000}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌
     *
     * @param userId   用户UUID
     * @param username 用户名
     * @param name     真实姓名
     * @param role     角色
     * @return JWT令牌
     */
    public String generateToken(String userId, String username, String name, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("name", name);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     *
     * @param userId 用户UUID
     * @return 刷新令牌
     */
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析令牌
     *
     * @param token JWT令牌
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            throw new JwtValidationException("Token expired", e);
        } catch (JwtException e) {
            log.warn("JWT令牌无效: {}", e.getMessage());
            throw new JwtValidationException("Invalid token", e);
        }
    }

    /**
     * 验证令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从令牌中获取用户UUID
     *
     * @param token JWT令牌
     * @return 用户UUID
     */
    public String getUserId(String token) {
        Claims claims = parseToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        // 兼容旧token中的Integer类型和新的String类型
        return String.valueOf(userIdObj);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从令牌中获取用户真实姓名
     *
     * @param token JWT令牌
     * @return 用户真实姓名
     */
    public String getName(String token) {
        Claims claims = parseToken(token);
        return claims.get("name", String.class);
    }

    /**
     * 从令牌中获取角色
     *
     * @param token JWT令牌
     * @return 角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 检查令牌是否即将过期 (小于30分钟)
     *
     * @param token JWT令牌
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            return timeLeft < 30 * 60 * 1000; // 小于30分钟
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * JWT验证异常
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}