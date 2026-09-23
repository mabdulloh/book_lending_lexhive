package io.github.mabdulloh.booklending.dto.loan;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BorrowRequest(
        @NotNull UUID bookUuid,
        @NotNull UUID memberUuid
) {}