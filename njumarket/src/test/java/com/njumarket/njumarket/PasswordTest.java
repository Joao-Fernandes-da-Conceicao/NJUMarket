package com.njumarket.njumarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class PasswordTest {

    @Test
    public void testBCryptPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "123456";
        
        // 测试您数据库中的哈希值
        String dbHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        
        System.out.println("原始密码: " + rawPassword);
        System.out.println("数据库哈希: " + dbHash);
        System.out.println("验证结果: " + encoder.matches(rawPassword, dbHash));
        
        // 生成一个新的哈希值
        String newHash = encoder.encode(rawPassword);
        System.out.println("新生成的哈希: " + newHash);
        System.out.println("新哈希验证: " + encoder.matches(rawPassword, newHash));
        
        // 测试一些已知的BCrypt哈希值
        String knownGoodHash = "$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa";
        System.out.println("已知哈希验证: " + encoder.matches("123456", knownGoodHash));
    }
}
