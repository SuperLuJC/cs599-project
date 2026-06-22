package com.ccsurvey.common.config;

import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
    }

    private void initAdminUser() {
        User admin = userRepository.findByUsername("admin");
        if (admin != null) {
            // 重置管理员密码为 admin123
            String encodedPassword = passwordEncoder.encode("admin123");
            admin.setPassword(encodedPassword);
            admin.setLoginFailCount(0);
            admin.setLockedUntil(null);
            userRepository.updateById(admin);
            log.info("管理员密码已重置为: admin123");
        }
    }
}
