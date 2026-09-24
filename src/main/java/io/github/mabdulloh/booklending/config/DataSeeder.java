package io.github.mabdulloh.booklending.config;

import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.repository.MemberRepository;
import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String adminUsername;
    @Value("${app.seed.admin.password:admin123}")
    private String adminPassword;
    @Value("${app.seed.member.email:member@example.com}")
    private String memberEmail;
    @Value("${app.seed.member.name:Demo Member}")
    private String memberName;
    @Value("${app.seed.member.password:member123}")
    private String memberPassword;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedMember();
    }

    private void seedAdmin() {
        if (userRepository.countByUsernameIncludingDeleted(adminUsername) > 0) {
            return;
        }
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setUsername(adminUsername);
        user.setPasswordHash(passwordEncoder.encode(adminPassword));
        user.setRole("ADMIN");
        userRepository.save(user);
        log.info("Seeded admin user: {}", adminUsername);
    }

    private void seedMember() {
        if (memberRepository.existsByEmail(memberEmail)
                || userRepository.countByUsernameIncludingDeleted(memberEmail) > 0) {
            return;
        }
        Member member = new Member();
        member.setUuid(UUID.randomUUID());
        member.setName(memberName);
        member.setEmail(memberEmail);
        member = memberRepository.save(member);

        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setUsername(memberEmail);
        user.setPasswordHash(passwordEncoder.encode(memberPassword));
        user.setRole("MEMBER");
        user.setMember(member);
        userRepository.save(user);
        log.info("Seeded member uuid={} with linked user", member.getUuid());
    }
}
