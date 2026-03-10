package com.pkms.config;

import com.pkms.model.User;
import com.pkms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        // Создаем тестового пользователя если его нет
        if (userRepository.findByUsername("test").isEmpty()) {
            User testUser = new User();
            testUser.setUsername("test");
            testUser.setEmail("test@example.com");
            testUser.setName("Test User");
            userRepository.save(testUser);
            System.out.println("✅ Test user created");
        }
    }
}