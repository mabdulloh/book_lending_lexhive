package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("select l from Loan l where l.uuid = :uuid")
    Optional<Loan> findByUuid(UUID uuid);

    @Query("select l from Loan l where l.member.uuid = :memberUuid")
    List<Loan> findByMemberUuid(UUID memberUuid);

    @Query("select count(l) from Loan l where l.member.uuid = :memberUuid and l.returnedAt is null")
    long countActiveByMemberUuid(UUID memberUuid);

    @Query("select l from Loan l where l.member.uuid = :memberUuid and l.returnedAt is null and l.dueDate < :now")
    List<Loan> findOverdueByMember(UUID memberUuid, Instant now);

    @Query("select l from Loan l where l.returnedAt is null and l.dueDate < :now")
    List<Loan> findAllOverdue(Instant now);
}