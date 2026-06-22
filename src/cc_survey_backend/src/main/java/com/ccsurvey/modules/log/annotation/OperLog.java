package com.ccsurvey.modules.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 模块标题
     */
    String title() default "";

    /**
     * 业务类型
     */
    String businessType() default "OTHER";
}