package com.ccsurvey.modules.user.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.log.annotation.OperLog;
import com.ccsurvey.modules.user.dto.UserDTO;
import com.ccsurvey.modules.user.dto.UserUpdateRequest;
import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器 (管理员)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping
    @OperLog(title = "用户管理", businessType = "SELECT")
    public ApiResponse<PageResponse<UserDTO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {

        PageResponse<UserDTO> result = userService.getUserList(page, size, keyword, role);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{uuid}")
    public ApiResponse<UserDTO> getUser(@PathVariable String uuid) {
        UserDTO user = userService.getUserByUuid(uuid);
        return ApiResponse.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @OperLog(title = "用户管理", businessType = "INSERT")
    public ApiResponse<UserDTO> createUser(@RequestBody User user) {
        UserDTO result = userService.createUser(user);
        return ApiResponse.success("用户创建成功", result);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{uuid}")
    @OperLog(title = "用户管理", businessType = "UPDATE")
    public ApiResponse<UserDTO> updateUser(
            @PathVariable String uuid,
            @Valid @RequestBody UserUpdateRequest request) {

        UserDTO result = userService.updateUser(uuid, request);
        return ApiResponse.success("用户更新成功", result);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{uuid}")
    @OperLog(title = "用户管理", businessType = "DELETE")
    public ApiResponse<Void> deleteUser(@PathVariable String uuid) {
        userService.deleteUser(uuid);
        return ApiResponse.success("用户删除成功");
    }

    /**
     * 重置密码
     */
    @PostMapping("/{uuid}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable String uuid,
            @RequestParam @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间") String newPassword) {

        userService.resetPassword(uuid, newPassword);
        return ApiResponse.success("密码重置成功");
    }

    /**
     * 切换用户状态
     */
    @PostMapping("/{uuid}/toggle-status")
    public ApiResponse<Void> toggleStatus(@PathVariable String uuid) {
        userService.toggleStatus(uuid);
        return ApiResponse.success("状态切换成功");
    }
}