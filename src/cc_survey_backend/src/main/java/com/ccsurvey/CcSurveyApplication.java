package com.ccsurvey;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CC Survey 系统主应用入口
 *
 * @author CC Survey Team
 */
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.ccsurvey.modules.*.repository")
public class CcSurveyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcSurveyApplication.class, args);
    }
}