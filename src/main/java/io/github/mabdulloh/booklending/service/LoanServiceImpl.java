package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.config.BorrowingRulesConfig;
import io.github.mabdulloh.booklending.domain.Book;
import io.github.mabdulloh.booklending.domain.Loan;
import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.dto.loan.LoanResponse;
import io.github.mabdulloh.booklending.exception.BookUnavailableException;
import io.github.mabdulloh.booklending.exception.EntityNotFoundException;
import io.github.mabdulloh.booklending.exception.LoanOwnershipException;
import io.github.mabdulloh.booklending.exception.MaxActiveLoansExceededException;
import io.github.mabdulloh.booklending.exception.MemberHasOverdueLoanException;
import io.github.mabdulloh.booklending.repository.LoanRepository;
import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService, BorrowingRulesService {

    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final MemberService memberService;
    private final UserRepository userRepository;
    private final BorrowingRulesConfig rulesConfig;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Override
    @Transactional
    public LoanResponse borrow(BorrowRequest req) {
        log.info("Borrow request: book={}, member={}", req.bookUuid(), req.memberUuid());

        Book book = bookService.requireByUuid(req.bookUuid());
        Member member = memberService.requireByUuid(req.memberUuid());

        validateCanBorrow(member.getUuid());
        validateBookAvailable(book);

        Instant now = Instant.now();
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(now);
        loan.setDueDate(computeDueDate(now));

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        Loan saved = loanRepository.save(loan);
        return LoanResponse.from(saved);
    }

    @Override
    @Transactional
    public LoanResponse returnLoan(UUID loanUuid) {
        log.info("Returning loan: {}", loanUuid);

        Loan loan = loanRepository.findByUuid(loanUuid)
                .orElseThrow(() -> new EntityNotFoundException("Loan", loanUuid));

        if (loan.getReturnedAt() != null) {
            log.warn("Loan {} already returned", loanUuid);
            return LoanResponse.from(loan);
        }

        if (!isAdmin() && !isLoanOwner(loan)) {
            log.warn("User {} attempted to return loan {} owned by member {}",
                    currentUsername(), loanUuid, loan.getMember().getUuid());
            throw new LoanOwnershipException();
        }

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);

        loan.setReturnedAt(Instant.now());
        return LoanResponse.from(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> listByMember(UUID memberUuid) {
        return loanRepository.findByMemberUuid(memberUuid).stream()
                .map(LoanResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> listOverdue() {
        return loanRepository.findAllOverdue(Instant.now()).stream()
                .map(LoanResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanBorrow(UUID memberUuid) {
        long active = loanRepository.countActiveByMemberUuid(memberUuid);
        if (active >= rulesConfig.getMaxActiveLoansPerMember()) {
            throw new MaxActiveLoansExceededException(memberUuid, rulesConfig.getMaxActiveLoansPerMember());
        }
        List<Loan> overdue = loanRepository.findOverdueByMember(memberUuid, Instant.now());
        if (!overdue.isEmpty()) {
            throw new MemberHasOverdueLoanException(memberUuid);
        }
    }

    @Override
    public Duration loanDuration() {
        return Duration.ofDays(rulesConfig.getLoanDurationDays());
    }

    private void validateBookAvailable(Book book) {
        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(book.getUuid());
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private boolean isLoanOwner(Loan loan) {
        String username = currentUsername();
        if (username == null) return false;
        return userRepository.findByUsername(username)
                .map(u -> loan.getMember().equals(u.getMember()))
                .orElse(false);
    }
}