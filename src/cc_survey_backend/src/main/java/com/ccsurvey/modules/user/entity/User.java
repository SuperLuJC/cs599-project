package com.ccsurvey.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class User {

    /**
     * UUID主键 (32位无横线)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 密码 (BCrypt加密)
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 真实姓名
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 角色: admin, user
     */
    private String role;

    /**
     * 状态: 1-正常, 0-禁用
     */
    private Integer status;

    /**
     * 邮箱是否已验证
     */
    private Integer emailVerified;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 账户锁定截止时间
     */
    private LocalDateTime lockedUntil;

    /**
     * 密码重置令牌
     */
    private String passwordResetToken;

    /**
     * 重置令牌过期时间
     */
    private LocalDateTime passwordResetExpires;

    /**
     * 邮箱验证令牌
     */
    private String emailVerifyToken;

    /**
     * 邮箱验证令牌过期时间
     */
    private LocalDateTime emailVerifyExpires;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 业务方法 ====================

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return "admin".equals(this.role);
    }

    /**
     * 是否正常状态
     */
    public boolean isActive() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 是否被锁定
     */
    public boolean isLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * 是否邮箱已验证
     */
    public boolean hasEmailVerified() {
        return Integer.valueOf(1).equals(this.emailVerified);
    }
}
