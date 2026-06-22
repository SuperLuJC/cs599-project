package com.ccsurvey.modules.auth.service;

import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.util.JwtUtils;
import com.ccsurvey.common.util.RedisUtils;
import com.ccsurvey.modules.auth.dto.LoginRequest;
import com.ccsurvey.modules.auth.dto.LoginResponse;
import com.ccsurvey.modules.auth.dto.RegisterRequest;
import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    @Value("${login.max-fail-count:5}")
    private int maxFailCount;

    @Value("${login.lock-duration:1800}")
    private int lockDurationSeconds;

    @Value("${jwt.expiration:7200000}")
    private long jwtExpiration;

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 检查账户状态
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 检查是否被锁定
        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 登录失败，增加失败次数
            userRepository.incrementLoginFailCount(user.getId());

            // 检查是否需要锁定
            if (user.getLoginFailCount() + 1 >= maxFailCount) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusSeconds(lockDurationSeconds);
                userRepository.lockAccount(user.getId(), lockedUntil);
                log.warn("用户 {} 登录失败次数达到上限，账户已锁定", user.getUsername());
            }

            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 登录成功，重置失败次数
        userRepository.resetLoginFailCount(user.getId());

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.updateById(user);

        // 生成JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getName(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 存储会话到Redis
        redisUtils.storeUserSession(user.getId(), token, jwtExpiration);

        // 返回用户信息
        return LoginResponse.builder()
                .uuid(user.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .needVerifyEmail(!user.hasEmailVerified())
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名是否存在
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 检查邮箱是否存在
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setName(request.getName()); // 使用用户填写的姓名
        user.setRole("user");
        user.setStatus(1);
        user.setEmailVerified(0);
        user.setLoginFailCount(0);

        // 生成邮箱验证令牌
        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        user.setEmailVerifyToken(verifyToken);
        user.setEmailVerifyExpires(LocalDateTime.now().plusHours(24));

        userRepository.insert(user);

        log.info("用户注册成功: {}", user.getUsername());

        // TODO: 发送验证邮件 (通过RabbitMQ)
    }

    /**
     * 用户登出
     */
    public void logout(String userId, String token) {
        // 将Token加入黑名单
        redisUtils.addToBlacklist(token, jwtExpiration);

        // 清除会话
        redisUtils.invalidateSession(userId);

        log.info("用户登出: userId={}", userId);
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String userId = jwtUtils.getUserId(refreshToken);

        // 检查用户是否存在
        User user = userRepository.selectById(userId);
        if (user == null || !user.isActive()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 生成新的访问令牌
        String newToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getName(), user.getRole());

        // 存储新会话
        redisUtils.storeUserSession(user.getId(), newToken, jwtExpiration);

        return newToken;
    }

    /**
     * 验证邮箱
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token);
        if (user == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        if (user.getEmailVerifyExpires() != null && user.getEmailVerifyExpires().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        user.setEmailVerified(1);
        user.setEmailVerifyToken(null);
        user.setEmailVerifyExpires(null);
        userRepository.updateById(user);

        log.info("邮箱验证成功: {}", user.getEmail());
    }

    /**
     * 请求密码重置
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            // 不暴露用户是否存在的信息
            log.info("密码重置请求: email={}, 用户不存在", email);
            return;
        }

        // 生成重置令牌
        String resetToken = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
        userRepository.updateById(user);

        log.info("密码重置令牌已生成: {}", user.getUsername());

        // TODO: 发送重置邮件 (通过RabbitMQ)
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        if (user.getPasswordResetExpires() != null && user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userRepository.updateById(user);

        // 清除所有会话
        redisUtils.invalidateSession(user.getId());

        log.info("密码重置成功: {}", user.getUsername());
    }
}