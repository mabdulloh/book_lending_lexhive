package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.dto.loan.LoanResponse;
import io.github.mabdulloh.booklending.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(req));
    }

    @PostMapping("/{uuid}/return")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public LoanResponse returnLoan(@PathVariable UUID uuid) {
        return loanService.returnLoan(uuid);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public List<LoanResponse> listByMember(@RequestParam("memberId") UUID memberUuid) {
        return loanService.listByMember(memberUuid);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LoanResponse> overdue() {
        return loanService.listOverdue();
    }
}