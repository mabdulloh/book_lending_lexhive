package io.github.mabdulloh.booklending.exception;

import java.util.UUID;

public class MaxActiveLoansExceededException extends RuntimeException {
    public MaxActiveLoansExceededException(UUID memberUuid, int max) {
        super("Member " + memberUuid + " reached max active loans: " + max);
    }
}