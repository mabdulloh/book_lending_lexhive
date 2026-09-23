package io.github.mabdulloh.booklending.dto.loan;

import io.github.mabdulloh.booklending.domain.Loan;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID uuid,
        UUID bookUuid,
        UUID memberUuid,
        Instant borrowedAt,
        Instant dueDate,
        Instant returnedAt
) {
    public static LoanResponse from(Loan l) {
        return new LoanResponse(
                l.getUuid(),
                l.getBook().getUuid(),
                l.getMember().getUuid(),
                l.getBorrowedAt(),
                l.getDueDate(),
                l.getReturnedAt()
        );
    }
}