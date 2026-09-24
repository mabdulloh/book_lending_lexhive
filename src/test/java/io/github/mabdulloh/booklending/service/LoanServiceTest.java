package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.config.BorrowingRulesConfig;
import io.github.mabdulloh.booklending.domain.Book;
import io.github.mabdulloh.booklending.domain.Loan;
import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.dto.loan.BorrowRequest;
import io.github.mabdulloh.booklending.exception.*;
import io.github.mabdulloh.booklending.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookService bookService;
    @Mock
    private MemberService memberService;
    @Mock
    private io.github.mabdulloh.booklending.repository.UserRepository userRepository;

    private BorrowingRulesConfig rules;
    private LoanServiceImpl loanService;

    private UUID bookUuid;
    private UUID memberUuid;
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() {
        rules = new BorrowingRulesConfig();
        rules.setMaxActiveLoansPerMember(3);
        rules.setLoanDurationDays(14);

        loanService = new LoanServiceImpl(loanRepository, bookService, memberService, userRepository, rules);

        setAuth("admin", "ROLE_ADMIN");

        bookUuid = UUID.randomUUID();
        memberUuid = UUID.randomUUID();

        book = new Book();
        book.setUuid(bookUuid);
        book.setTotalCopies(5);
        book.setAvailableCopies(2);

        member = new Member();
        member.setUuid(memberUuid);
    }

    @Test
    @DisplayName("borrow - success - availableCopies decremented, dueDate set")
    void borrow_success() {
        when(bookService.requireByUuid(bookUuid)).thenReturn(book);
        when(memberService.requireByUuid(memberUuid)).thenReturn(member);
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(0L);
        when(loanRepository.findOverdueByMember(eq(memberUuid), any())).thenReturn(List.of());
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        var resp = loanService.borrow(new BorrowRequest(bookUuid, memberUuid));

        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        Loan saved = captor.getValue();
        assertThat(saved.getBook()).isSameAs(book);
        assertThat(saved.getMember()).isSameAs(member);
        assertThat(saved.getBorrowedAt()).isAfterOrEqualTo(before);
        assertThat(saved.getDueDate()).isAfterOrEqualTo(before.plus(13, ChronoUnit.DAYS));
        assertThat(book.getAvailableCopies()).isEqualTo(1);
        assertThat(resp.bookUuid()).isEqualTo(bookUuid);
        assertThat(resp.memberUuid()).isEqualTo(memberUuid);
        assertThat(resp.returnedAt()).isNull();
    }

    @Test
    @DisplayName("borrow - max active loans reached - throws")
    void borrow_maxActiveLoansExceeded() {
        when(bookService.requireByUuid(bookUuid)).thenReturn(book);
        when(memberService.requireByUuid(memberUuid)).thenReturn(member);
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(3L);

        assertThatThrownBy(() -> loanService.borrow(new BorrowRequest(bookUuid, memberUuid)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanRepository, never()).save(any());
        assertThat(book.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("borrow - member has overdue loan - throws")
    void borrow_overdueLoan() {
        when(bookService.requireByUuid(bookUuid)).thenReturn(book);
        when(memberService.requireByUuid(memberUuid)).thenReturn(member);
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(0L);
        when(loanRepository.findOverdueByMember(eq(memberUuid), any()))
                .thenReturn(List.of(new Loan()));

        assertThatThrownBy(() -> loanService.borrow(new BorrowRequest(bookUuid, memberUuid)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrow - book has no available copies - throws")
    void borrow_bookUnavailable() {
        book.setAvailableCopies(0);
        when(bookService.requireByUuid(bookUuid)).thenReturn(book);
        when(memberService.requireByUuid(memberUuid)).thenReturn(member);
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(0L);
        when(loanRepository.findOverdueByMember(eq(memberUuid), any())).thenReturn(List.of());

        assertThatThrownBy(() -> loanService.borrow(new BorrowRequest(bookUuid, memberUuid)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("returnLoan - success - availableCopies incremented, returnedAt set")
    void returnLoan_success() {
        UUID loanUuid = UUID.randomUUID();
        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        loan.setDueDate(Instant.now().plus(9, ChronoUnit.DAYS));

        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        Instant before = Instant.now();
        var resp = loanService.returnLoan(loanUuid);

        assertThat(book.getAvailableCopies()).isEqualTo(3);
        assertThat(loan.getReturnedAt()).isNotNull();
        assertThat(loan.getReturnedAt()).isAfterOrEqualTo(before);
        assertThat(resp.returnedAt()).isNotNull();
    }

    @Test
    @DisplayName("returnLoan - idempotent - second return does not double-increment")
    void returnLoan_idempotent() {
        UUID loanUuid = UUID.randomUUID();
        Instant firstReturn = Instant.now().minus(1, ChronoUnit.HOURS);
        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setReturnedAt(firstReturn);

        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        var resp = loanService.returnLoan(loanUuid);

        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(loan.getReturnedAt()).isEqualTo(firstReturn);
        assertThat(resp.returnedAt()).isEqualTo(firstReturn);
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("returnLoan - loan not found - throws")
    void returnLoan_notFound() {
        UUID loanUuid = UUID.randomUUID();
        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(loanUuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("returnLoan - member owns loan - returns successfully")
    void returnLoan_memberOwnsLoan() {
        UUID loanUuid = UUID.randomUUID();
        long memberInternalId = 42L;
        member.setId(memberInternalId);

        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        loan.setDueDate(Instant.now().plus(9, ChronoUnit.DAYS));

        setAuth("member-owner", "ROLE_MEMBER");

        User linkedUser = new User();
        linkedUser.setId(99L);
        linkedUser.setMember(member);
        when(userRepository.findByUsername("member-owner")).thenReturn(Optional.of(linkedUser));
        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        var resp = loanService.returnLoan(loanUuid);

        assertThat(resp.returnedAt()).isNotNull();
        assertThat(book.getAvailableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("returnLoan - member does not own loan - throws LoanOwnershipException")
    void returnLoan_memberNotOwner() {
        UUID loanUuid = UUID.randomUUID();
        member.setId(42L);

        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(Instant.now());
        loan.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));

        setAuth("member-stranger", "ROLE_MEMBER");

        User stranger = new User();
        stranger.setId(100L);
        stranger.setMember(null);
        when(userRepository.findByUsername("member-stranger")).thenReturn(Optional.of(stranger));
        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(loanUuid))
                .isInstanceOf(LoanOwnershipException.class);
    }

    @Test
    @DisplayName("returnLoan - user not found in repo - treated as no owner")
    void returnLoan_userNotFoundInRepo() {
        UUID loanUuid = UUID.randomUUID();
        member.setId(42L);

        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(Instant.now());
        loan.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));

        setAuth("orphan-user", "ROLE_MEMBER");

        when(userRepository.findByUsername("orphan-user")).thenReturn(Optional.empty());
        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(loanUuid))
                .isInstanceOf(LoanOwnershipException.class);
    }

    @Test
    @DisplayName("returnLoan - admin bypasses ownership regardless of user.memberId")
    void returnLoan_adminBypassesOwnership() {
        UUID loanUuid = UUID.randomUUID();
        member.setId(42L);

        Loan loan = new Loan();
        loan.setUuid(loanUuid);
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowedAt(Instant.now());
        loan.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));

        setAuth("admin", "ROLE_ADMIN");

        when(loanRepository.findByUuid(loanUuid)).thenReturn(Optional.of(loan));

        var resp = loanService.returnLoan(loanUuid);

        assertThat(resp.returnedAt()).isNotNull();
        assertThat(book.getAvailableCopies()).isEqualTo(3);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("listByMember - returns mapped loans")
    void listByMember_ok() {
        Loan loan = new Loan();
        loan.setUuid(UUID.randomUUID());
        loan.setBook(book);
        loan.setMember(member);
        when(loanRepository.findByMemberUuid(memberUuid)).thenReturn(List.of(loan));

        var list = loanService.listByMember(memberUuid);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).bookUuid()).isEqualTo(bookUuid);
        assertThat(list.get(0).memberUuid()).isEqualTo(memberUuid);
    }

    @Test
    @DisplayName("listOverdue - returns mapped loans using now()")
    void listOverdue_ok() {
        Loan loan = new Loan();
        loan.setUuid(UUID.randomUUID());
        loan.setBook(book);
        loan.setMember(member);
        when(loanRepository.findAllOverdue(any())).thenReturn(List.of(loan));

        var list = loanService.listOverdue();

        assertThat(list).hasSize(1);
        verify(loanRepository, times(1)).findAllOverdue(any());
    }

    @Test
    @DisplayName("validateCanBorrow - ok - no exceptions")
    void validateCanBorrow_ok() {
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(1L);
        when(loanRepository.findOverdueByMember(eq(memberUuid), any())).thenReturn(List.of());

        loanService.validateCanBorrow(memberUuid);
    }

    @Test
    @DisplayName("validateCanBorrow - max reached - throws")
    void validateCanBorrow_max() {
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(3L);

        assertThatThrownBy(() -> loanService.validateCanBorrow(memberUuid))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("validateCanBorrow - overdue - throws")
    void validateCanBorrow_overdue() {
        when(loanRepository.countActiveByMemberUuid(memberUuid)).thenReturn(0L);
        when(loanRepository.findOverdueByMember(eq(memberUuid), any())).thenReturn(List.of(new Loan()));

        assertThatThrownBy(() -> loanService.validateCanBorrow(memberUuid))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static void setAuth(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}