package io.github.mabdulloh.booklending.exception;

public class LoanOwnershipException extends RuntimeException {
    public LoanOwnershipException() {
        super("loan does not belong to authenticated user");
    }
}