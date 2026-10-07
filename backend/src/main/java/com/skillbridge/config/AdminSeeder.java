package com.skillbridge.config;

import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmail("admin@skillbridge.com").isEmpty()) {
                User admin = new User();
                admin.setName("SkillBridge Admin");
                admin.setEmail("admin@skillbridge.com");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setPhone("");
                admin.setAddress("");
                users.save(admin);
            }
        };
    }
}
