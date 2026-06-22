package com.ccsurvey.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 文件访问路径映射
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./uploads/", "file:./temp/");

        // 静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * 拦截器配置
     * 注意: 认证拦截器在 SecurityConfig 中配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 可以在这里添加其他拦截器
        // 认证和限流拦截器通过 Filter 实现，优先级更高
    }
}