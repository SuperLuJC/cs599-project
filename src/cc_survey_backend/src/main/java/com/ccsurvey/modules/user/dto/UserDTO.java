package com.ccsurvey.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String uuid;
    private String username;
    private String email;
    private String phone;
    private String name;
    private String avatar;
    private String role;
    private Integer status;
    private Integer emailVerified;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}