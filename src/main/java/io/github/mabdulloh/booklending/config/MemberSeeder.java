package io.github.mabdulloh.booklending.config;

import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.repository.MemberRepository;
import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class MemberSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.member.email:member@example.com}")
    private String memberEmail;
    @Value("${app.seed.member.name:Demo Member}")
    private String memberName;
    @Value("${app.seed.member.password:member123}")
    private String memberPassword;

    @Override
    public void run(String... args) {
        if (memberRepository.existsByEmailAndDeletedAtIsNull(memberEmail)) {
            return;
        }
        if (userRepository.findByUsernameAndDeletedAtIsNull(memberEmail).isPresent()) {
            return;
        }

        Member member = new Member();
        member.setUuid(UUID.randomUUID());
        member.setName(memberName);
        member.setEmail(memberEmail);
        member = memberRepository.save(member);
        log.info("Seeded member uuid={}, id={}", member.getUuid(), member.getId());

        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setUsername(memberEmail);
        user.setPasswordHash(passwordEncoder.encode(memberPassword));
        user.setRole("MEMBER");
        user.setMemberId(member.getId());
        userRepository.save(user);
        log.info("Seeded user linked to member id={}", member.getId());
    }
}