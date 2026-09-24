package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUuid(UUID uuid);

    boolean existsByEmail(String email);
}