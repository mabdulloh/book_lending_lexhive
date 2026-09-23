package io.github.mabdulloh.booklending.exception;

import java.util.UUID;

public class BookUnavailableException extends RuntimeException {
    public BookUnavailableException(UUID bookUuid) {
        super("No available copies for book: " + bookUuid);
    }
}