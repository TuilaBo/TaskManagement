package com.example.TaskManagement.config;

import com.example.TaskManagement.model.TaskCategory;
import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import com.example.TaskManagement.repository.TaskCategoryRepository;
import com.example.TaskManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedManager();
        seedCategories();
    }

    private void seedManager() {
        if (userRepository.existsByRole(Role.MANAGER)) {
            return;
        }

        userRepository.save(User.builder()
                .username("manager")
                .fullName("Người Quản Lý")
                .password(passwordEncoder.encode("manager123"))
                .email("manager@task.com")
                .role(Role.MANAGER)
                .active(true)
                .build());

        log.info("Default manager account seeded: manager / manager123");
    }

    private void seedCategories() {
        if (taskCategoryRepository.count() > 0) {
            return;
        }

        taskCategoryRepository.save(TaskCategory.builder().name("Báo cáo").description("Công việc báo cáo định kỳ").build());
        taskCategoryRepository.save(TaskCategory.builder().name("Họp").description("Chuẩn bị và tham dự họp").build());
        taskCategoryRepository.save(TaskCategory.builder().name("Kỹ thuật").description("Công việc kỹ thuật / phát triển").build());

        log.info("Default task categories seeded");
    }
}
