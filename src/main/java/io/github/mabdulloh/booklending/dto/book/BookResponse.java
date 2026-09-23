package io.github.mabdulloh.booklending.dto.book;

import io.github.mabdulloh.booklending.domain.Book;

import java.time.Instant;
import java.util.UUID;

public record BookResponse(
        UUID uuid,
        String title,
        String author,
        String isbn,
        int totalCopies,
        int availableCopies,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookResponse from(Book b) {
        return new BookResponse(
                b.getUuid(),
                b.getTitle(),
                b.getAuthor(),
                b.getIsbn(),
                b.getTotalCopies(),
                b.getAvailableCopies(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}