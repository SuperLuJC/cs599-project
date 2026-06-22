package com.ccsurvey.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.user.dto.UserDTO;
import com.ccsurvey.modules.user.dto.UserUpdateRequest;
import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户列表 (分页)
     */
    public PageResponse<UserDTO> getUserList(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getEmail, keyword)
            );
        }

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageResult = userRepository.selectPage(new Page<>(page, size), wrapper);

        List<UserDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponse.of(dtoList, pageResult.getTotal(), page, size);
    }

    /**
     * 获取用户详情
     */
    public UserDTO getUserByUuid(String uuid) {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToDTO(user);
    }

    /**
     * 创建用户 (管理员)
     */
    @Transactional
    public UserDTO createUser(User user) {
        // 检查用户名是否存在
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 检查邮箱是否存在
        if (user.getEmail() != null && userRepository.findByEmail(user.getEmail()) != null) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        // 设置默认值
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setEmailVerified(0);
        user.setLoginFailCount(0);

        if (user.getRole() == null) {
            user.setRole("user");
        }

        userRepository.insert(user);

        log.info("用户创建成功: username={}", user.getUsername());

        return convertToDTO(user);
    }

    /**
     * 更新用户
     */
    @Transactional
    public UserDTO updateUser(String uuid, UserUpdateRequest request) {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查邮箱是否被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            User existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getId().equals(uuid)) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(0); // 更新邮箱需要重新验证
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getRole() != null) user.setRole(request.getRole());

        userRepository.updateById(user);

        log.info("用户更新成功: uuid={}", uuid);

        return convertToDTO(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(String uuid) {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 不能删除自己
        // if (user.getId().equals(currentUserId)) {
        //     throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除自己");
        // }

        userRepository.deleteById(uuid);

        log.info("用户删除成功: uuid={}", uuid);
    }

    /**
     * 重置用户密码
     */
    @Transactional
    public void resetPassword(String uuid, String newPassword) {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginFailCount(0);
        user.setLockedUntil(null);

        userRepository.updateById(user);

        log.info("用户密码重置: uuid={}", uuid);
    }

    /**
     * 切换用户状态
     */
    @Transactional
    public void toggleStatus(String uuid) {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userRepository.updateById(user);

        log.info("用户状态切换: uuid={}, status={}", uuid, user.getStatus());
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .uuid(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .build();
    }
}