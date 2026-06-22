package com.ccsurvey.common.response;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // ==================== 通用错误 (1000-1999) ====================
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ==================== 认证错误 (2000-2999) ====================
    INVALID_CREDENTIALS(2001, "用户名或密码错误"),
    TOKEN_EXPIRED(2002, "登录已过期，请重新登录"),
    TOKEN_INVALID(2003, "无效的登录凭证"),
    TOKEN_MISSING(2004, "缺少登录凭证"),
    ACCOUNT_LOCKED(2005, "账户已被锁定，请稍后再试"),
    ACCOUNT_DISABLED(2006, "账户已被禁用"),
    EMAIL_NOT_VERIFIED(2007, "邮箱未验证，请先验证邮箱"),
    LOGIN_TOO_FREQUENT(2008, "登录过于频繁，请稍后再试"),
    REFRESH_TOKEN_INVALID(2009, "刷新令牌无效"),
    REFRESH_TOKEN_EXPIRED(2010, "刷新令牌已过期"),

    // ==================== 用户错误 (3000-3999) ====================
    USER_NOT_FOUND(3001, "用户不存在"),
    USERNAME_EXISTS(3002, "用户名已存在"),
    EMAIL_EXISTS(3003, "邮箱已被注册"),
    PHONE_EXISTS(3004, "手机号已被注册"),
    PASSWORD_MISMATCH(3005, "两次密码不一致"),
    PASSWORD_TOO_WEAK(3006, "密码强度不足"),
    OLD_PASSWORD_ERROR(3007, "原密码错误"),
    VERIFICATION_CODE_ERROR(3008, "验证码错误"),
    VERIFICATION_CODE_EXPIRED(3009, "验证码已过期"),
    VERIFICATION_CODE_SENT(3010, "验证码已发送，请稍后再试"),

    // ==================== 问卷错误 (4000-4999) ====================
    SURVEY_NOT_FOUND(4001, "问卷不存在"),
    SURVEY_NOT_PUBLISHED(4002, "问卷未发布"),
    SURVEY_EXPIRED(4003, "问卷已过期"),
    SURVEY_NOT_STARTED(4004, "问卷尚未开始"),
    SURVEY_ALREADY_SUBMITTED(4005, "您已提交过该问卷"),
    SUBMISSION_LIMIT_REACHED(4006, "问卷提交次数已达上限"),
    SURVEY_SCHEMA_INVALID(4007, "问卷格式无效"),
    SURVEY_DRAFT_ONLY(4008, "问卷为草稿状态，无法填写"),
    SURVEY_ARCHIVED(4009, "问卷已归档"),

    // ==================== 文件错误 (5000-5999) ====================
    FILE_EMPTY(5001, "文件不能为空"),
    FILE_TOO_LARGE(5002, "文件大小超过限制"),
    FILE_TYPE_NOT_ALLOWED(5003, "文件类型不允许"),
    FILE_UPLOAD_FAILED(5004, "文件上传失败"),
    FILE_NOT_FOUND(5005, "文件不存在"),
    FILE_DELETE_FAILED(5006, "文件删除失败"),
    FILE_NAME_INVALID(5007, "文件名包含非法字符"),

    // ==================== 限流错误 (6000-6999) ====================
    RATE_LIMIT_EXCEEDED(6001, "请求过于频繁，请稍后再试"),
    API_RATE_LIMIT_EXCEEDED(6002, "API调用频率超限"),
    SUBMISSION_RATE_LIMIT_EXCEEDED(6003, "提交过于频繁"),

    // ==================== 业务错误 (7000-7999) ====================
    SUBMISSION_IN_PROGRESS(7001, "正在处理中，请勿重复提交"),
    EXPORT_IN_PROGRESS(7002, "导出任务进行中"),
    EXPORT_FAILED(7003, "导出失败"),
    EMAIL_SEND_FAILED(7004, "邮件发送失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}