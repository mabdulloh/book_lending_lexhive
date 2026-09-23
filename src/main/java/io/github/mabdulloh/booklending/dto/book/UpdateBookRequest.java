package io.github.mabdulloh.booklending.dto.book;

import jakarta.validation.constraints.*;

public record UpdateBookRequest(
        @Size(max = 200) String title,
        @Size(max = 120) String author,
        @Min(1) Integer totalCopies
) {}