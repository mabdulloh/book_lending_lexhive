package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.dto.member.MemberResponse;
import io.github.mabdulloh.booklending.exception.DuplicateEmailException;
import io.github.mabdulloh.booklending.exception.EntityNotFoundException;
import io.github.mabdulloh.booklending.repository.MemberRepository;
import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberResponse create(CreateMemberRequest req) {
        if (memberRepository.existsByEmailAndDeletedAtIsNull(req.email())) {
            log.info("Duplicate member email: {}", req.email());
            throw new DuplicateEmailException(req.email());
        }
        if (userRepository.findByUsernameAndDeletedAtIsNull(req.email()).isPresent()) {
            log.info("Username already taken: {}", req.email());
            throw new DuplicateEmailException(req.email());
        }

        Member member = new Member();
        member.setUuid(UUID.randomUUID());
        member.setName(req.name());
        member.setEmail(req.email());
        member = memberRepository.save(member);
        log.info("Created member uuid={}, id={}", member.getUuid(), member.getId());

        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setUsername(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole("MEMBER");
        user.setMember(member);
        userRepository.save(user);
        log.info("Created user linked to member id={}", member.getId());

        return MemberResponse.from(member);
    }

    @Override
    public MemberResponse get(UUID uuid) {
        return MemberResponse.from(findActiveByUuid(uuid));
    }

    @Override
    public List<MemberResponse> list() {
        return memberRepository.findAllActive().stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID uuid) {
        Member member = findActiveByUuid(uuid);
        member.setDeletedAt(Instant.now());
        memberRepository.save(member);
        userRepository.findByMemberIdAndDeletedAtIsNull(member.getId()).ifPresent(u -> {
            u.setDeletedAt(Instant.now());
            userRepository.save(u);
        });
        log.info("Soft-deleted member uuid={} and linked user", uuid);
    }

    @Override
    public Member requireByUuid(UUID uuid) {
        return findActiveByUuid(uuid);
    }

    private Member findActiveByUuid(UUID uuid) {
        return memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Member", uuid));
    }
}