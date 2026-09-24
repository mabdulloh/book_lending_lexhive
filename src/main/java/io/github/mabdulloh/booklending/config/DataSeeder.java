package io.github.mabdulloh.booklending.config;

import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String adminUsername;
    @Value("${app.seed.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedUser(adminUsername, adminPassword, "ADMIN");
    }

    private void seedUser(String username, String password, String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        log.info("Seeded user: {} ({})", username, role);
    }
}