package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.dto.loan.LoanResponse;
import io.github.mabdulloh.booklending.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "4. Loans", description = "Borrow and return books")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(
            summary = "Borrow a book",
            description = "Member-only. Validates borrowing rules (max active loans, overdue check, book availability). Decrements `availableCopies`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "uuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "bookUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "memberUuid": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "borrowedAt": "2026-09-23T13:00:00Z",
                                      "dueDate": "2026-10-07T13:00:00Z",
                                      "returnedAt": null
                                    }"""))),
            @ApiResponse(responseCode = "422", description = "Borrowing rules violated (max active loans / overdue / book unavailable)")
    })
    public ResponseEntity<LoanResponse> borrow(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Borrow payload",
                    schema = @Schema(implementation = BorrowRequest.class),
                    example = """
                            {
                              "bookUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                              "memberUuid": "4fa85f64-5717-4562-b3fc-2c963f66afa6"
                            }""")
            @Valid @RequestBody BorrowRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(req));
    }

    @PostMapping("/{uuid}/return")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(
            summary = "Return a loan",
            description = "Members can only return their own loans. Increments `availableCopies`. Idempotent (returning twice is a no-op)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returned",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "403", description = "Loan belongs to a different member"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public LoanResponse returnLoan(
            @Parameter(description = "Loan UUID") @PathVariable UUID uuid) {
        return loanService.returnLoan(uuid);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "List loans by member",
            description = "MEMBER users can only list their own loans (returns 403 if `memberId` does not match their `users.member_id`).")
    @ApiResponse(responseCode = "200", description = "Loans for the member",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    public List<LoanResponse> listByMember(
            @Parameter(description = "Member UUID") @RequestParam("memberId") UUID memberUuid) {
        return loanService.listByMember(memberUuid);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List overdue loans",
            description = "Loans where `returnedAt IS NULL` and `dueDate < now`.")
    @ApiResponse(responseCode = "200", description = "List of overdue loans",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    public List<LoanResponse> overdue() {
        return loanService.listOverdue();
    }
}