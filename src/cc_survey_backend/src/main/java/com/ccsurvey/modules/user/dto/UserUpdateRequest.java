package com.ccsurvey.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新请求DTO
 */
@Data
public class UserUpdateRequest {

    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String avatar;

    private Integer status;

    private String role;
}