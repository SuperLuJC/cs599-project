package com.ccsurvey.modules.agent.dto;

import lombok.Data;

/**
 * 消息信息
 */
@Data
public class MessageInfo {

    /**
     * 角色: user, assistant, tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private String createdAt;
}
