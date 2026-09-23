package io.github.mabdulloh.booklending.dto.book;

import jakarta.validation.constraints.*;

public record CreateBookRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 120) String author,
        @NotBlank @Size(max = 20) String isbn,
        @Min(0) int totalCopies
) {}