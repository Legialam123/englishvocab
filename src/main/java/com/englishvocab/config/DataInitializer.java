package com.englishvocab.config;

import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        //initializeUsers();
    }

    /*
    private void initializeUsers() {
        log.info("Bắt đầu khởi tạo dữ liệu user mẫu...");
        
        // Kiểm tra nếu đã có data thì không tạo lại
        if (userRepository.count() > 0) {
            log.info("Database đã có user, bỏ qua việc khởi tạo data mẫu.");
            return;
        }
        
        // 1. Tạo Admin User
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullname("Quản trị viên")
                    .email("admin@englishvocab.com")
                    .role(User.Role.ADMIN)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(adminUser);
            log.info("✅ Tạo thành công Admin user: admin / admin123");
        }
        
        // 2. Tạo User thường thứ 2
        if (!userRepository.existsByUsername("teacher")) {
            User teacherUser = User.builder()
                    .username("teacher")
                    .password(passwordEncoder.encode("teacher123"))
                    .fullname("Giáo viên Tiếng Anh")
                    .email("teacher@englishvocab.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(teacherUser);
            log.info("✅ Tạo thành công Teacher user: teacher / teacher123");
        }
        
        // 3. Tạo Normal User
        if (!userRepository.existsByUsername("user")) {
            User normalUser = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .fullname("Người dùng thường")
                    .email("user@englishvocab.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(normalUser);
            log.info("✅ Tạo thành công Normal user: user / user123");
        }
        
        // 4. Tạo thêm user demo
        if (!userRepository.existsByUsername("demo")) {
            User demoUser = User.builder()
                    .username("demo")
                    .password(passwordEncoder.encode("demo123"))
                    .fullname("Nguyễn Văn Demo")
                    .email("demo@example.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(demoUser);
            log.info("✅ Tạo thành công Demo user: demo / demo123");
        }
        
        // 5. Tạo user bị khóa để test
        if (!userRepository.existsByUsername("locked")) {
            User lockedUser = User.builder()
                    .username("locked")
                    .password(passwordEncoder.encode("locked123"))
                    .fullname("User bị khóa")
                    .email("locked@example.com")
                    .role(User.Role.USER)
                    .status(User.Status.LOCKED)
                    .build();
            userRepository.save(lockedUser);
            log.info("✅ Tạo thành công Locked user: locked / locked123 (không thể đăng nhập)");
        }
        
        long totalUsers = userRepository.count();
        log.info("🎉 Hoàn thành khởi tạo dữ liệu! Tổng số user: {}", totalUsers);
    }
    */
}
