package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("select m from Member m where m.uuid = :uuid and m.deletedAt is null")
    Optional<Member> findByUuid(UUID uuid);

    @Query("select m from Member m where m.deletedAt is null")
    List<Member> findAllActive();

    boolean existsByEmailAndDeletedAtIsNull(String email);
}