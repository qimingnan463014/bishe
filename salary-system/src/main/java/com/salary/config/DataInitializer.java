package com.salary.config;

import com.salary.entity.User;
import com.salary.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动后自动初始化基础数据
 * 确保 admin 账号存在且密码 Hash 从未被搞坏
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // 初始化超级管理员账号
        initAdmin();
    }

    private void initAdmin() {
        // 用 username 查找 admin（通过 MyBatis-Plus 的 XML 方法）
        User existing = userMapper.selectByUsernameWithPassword("admin");

        if (existing == null) {
            // 不存在则创建
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(1);
            admin.setRealName("系统管理员");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("✅ 管理员账号初始化完成 → 账号: admin 密码: admin");
        } else {
            // 存在则强制重置密码（保证哈希是用当前 PasswordEncoder 生成的）
            User update = new User();
            update.setId(existing.getId());
            update.setPassword(passwordEncoder.encode("admin"));
            userMapper.updateById(update);
            log.info("✅ 管理员密码已重置 → 账号: admin 密码: admin");
        }
    }
}
