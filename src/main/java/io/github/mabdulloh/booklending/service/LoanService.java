package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.dto.loan.LoanResponse;

import java.util.List;
import java.util.UUID;

public interface LoanService {
    LoanResponse borrow(BorrowRequest req);
    LoanResponse returnLoan(UUID loanUuid);
    List<LoanResponse> listByMember(UUID memberUuid);
    List<LoanResponse> listOverdue();
    void validateCanBorrow(UUID memberUuid);
}