package com.ccsurvey.modules.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {

    /**
     * 根据UUID查询用户
     */
    default User findByUuid(String uuid) {
        return selectById(uuid);
    }

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    /**
     * 根据邮箱验证令牌查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email_verify_token = #{token}")
    User findByEmailVerifyToken(@Param("token") String token);

    /**
     * 根据密码重置令牌查询用户
     */
    @Select("SELECT * FROM sys_user WHERE password_reset_token = #{token}")
    User findByPasswordResetToken(@Param("token") String token);

    /**
     * 更新登录失败次数
     */
    @Update("UPDATE sys_user SET login_fail_count = login_fail_count + 1 WHERE id = #{userId}")
    int incrementLoginFailCount(@Param("userId") String userId);

    /**
     * 重置登录失败次数
     */
    @Update("UPDATE sys_user SET login_fail_count = 0, locked_until = NULL WHERE id = #{userId}")
    int resetLoginFailCount(@Param("userId") String userId);

    /**
     * 锁定账户
     */
    @Update("UPDATE sys_user SET locked_until = #{lockedUntil} WHERE id = #{userId}")
    int lockAccount(@Param("userId") String userId, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);
}