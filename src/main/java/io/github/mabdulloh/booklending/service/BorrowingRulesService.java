package io.github.mabdulloh.booklending.service;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface BorrowingRulesService {

    void validateCanBorrow(UUID memberUuid);

    default Duration loanDuration() {
        return Duration.ofDays(14);
    }

    default Instant computeDueDate(Instant borrowedAt) {
        return borrowedAt.plus(loanDuration());
    }
}