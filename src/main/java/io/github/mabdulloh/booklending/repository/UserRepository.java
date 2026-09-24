package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByMemberId(Long memberId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE username = :username", nativeQuery = true)
    long countByUsernameIncludingDeleted(@Param("username") String username);
}
