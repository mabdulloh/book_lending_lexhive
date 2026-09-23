package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<User> findByMemberIdAndDeletedAtIsNull(Long memberId);
}