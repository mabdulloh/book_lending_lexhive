package io.github.mabdulloh.booklending.exception;

import java.util.UUID;

public class MemberHasOverdueLoanException extends RuntimeException {
    public MemberHasOverdueLoanException(UUID memberUuid) {
        super("Member " + memberUuid + " has overdue loans");
    }
}