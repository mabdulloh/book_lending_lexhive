package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Book;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;
import io.github.mabdulloh.booklending.exception.ConflictException;
import io.github.mabdulloh.booklending.exception.EntityNotFoundException;
import io.github.mabdulloh.booklending.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private UUID uuid;
    private Book existing;

    @BeforeEach
    void setUp() {
        uuid = UUID.randomUUID();
        existing = new Book();
        existing.setUuid(uuid);
        existing.setTitle("Old");
        existing.setAuthor("Old Author");
        existing.setIsbn("123");
        existing.setTotalCopies(5);
        existing.setAvailableCopies(2);
    }

    @Test
    @DisplayName("create - success - availableCopies equals totalCopies")
    void create_success() {
        when(bookRepository.existsByIsbn("999")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = bookService.create(new CreateBookRequest("T", "A", "999", 4));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book saved = captor.getValue();
        assertThat(saved.getTotalCopies()).isEqualTo(4);
        assertThat(saved.getAvailableCopies()).isEqualTo(4);
        assertThat(saved.getUuid()).isNotNull();
        assertThat(resp.availableCopies()).isEqualTo(4);
    }

    @Test
    @DisplayName("create - duplicate isbn throws ConflictException")
    void create_duplicateIsbn() {
        when(bookRepository.existsByIsbn("123")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(new CreateBookRequest("T", "A", "123", 1)))
                .isInstanceOf(ConflictException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("get - returns response when found")
    void get_found() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        var resp = bookService.get(uuid);

        assertThat(resp.uuid()).isEqualTo(uuid);
        assertThat(resp.title()).isEqualTo("Old");
    }

    @Test
    @DisplayName("get - throws EntityNotFoundException when missing")
    void get_notFound() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.get(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("list - returns mapped responses")
    void list_ok() {
        when(bookRepository.findAll()).thenReturn(List.of(existing));

        var list = bookService.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).uuid()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("update - partial - only title changed")
    void update_partialTitle() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = bookService.update(uuid, new UpdateBookRequest("New", null, null));

        assertThat(existing.getTitle()).isEqualTo("New");
        assertThat(existing.getAuthor()).isEqualTo("Old Author");
        assertThat(existing.getTotalCopies()).isEqualTo(5);
        verify(bookRepository, times(1)).save(existing);
        assertThat(resp.title()).isEqualTo("New");
    }

    @Test
    @DisplayName("update - totalCopies up - availableCopies increases by delta")
    void update_totalCopiesUp() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 7));

        assertThat(existing.getTotalCopies()).isEqualTo(7);
        assertThat(existing.getAvailableCopies()).isEqualTo(4);
    }

    @Test
    @DisplayName("update - totalCopies down within loans available - availableCopies decreases")
    void update_totalCopiesDownSafe() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 4));

        assertThat(existing.getTotalCopies()).isEqualTo(4);
        assertThat(existing.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    @DisplayName("update - totalCopies below active loans - throws IllegalArgumentException")
    void update_totalCopiesBelowActiveLoans() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.update(uuid, new UpdateBookRequest(null, null, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active loans");

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - totalCopies only - title and author unchanged")
    void update_totalCopiesOnly_keepsOtherFields() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 5));

        assertThat(existing.getTotalCopies()).isEqualTo(5);
        assertThat(existing.getTitle()).isEqualTo("Old");
        assertThat(existing.getAuthor()).isEqualTo("Old Author");
    }

    @Test
    @DisplayName("update - same totalCopies - no-op arithmetic, available unchanged")
    void update_totalCopiesSameValue() {
        existing.setAvailableCopies(2);
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 5));

        assertThat(existing.getTotalCopies()).isEqualTo(5);
        assertThat(existing.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("update - shrink with no active loans - allowed, available decreases")
    void update_shrinkNoActiveLoans() {
        existing.setTotalCopies(10);
        existing.setAvailableCopies(10);
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 5));

        assertThat(existing.getTotalCopies()).isEqualTo(5);
        assertThat(existing.getAvailableCopies()).isEqualTo(5);
    }

    @Test
    @DisplayName("update - negative delta when activeLoans > 0 - throws, never saves")
    void update_shrinkActiveLoans_throws() {
        existing.setTotalCopies(10);
        existing.setAvailableCopies(2);
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.update(uuid, new UpdateBookRequest(null, null, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8")
                .hasMessageContaining("5");

        verify(bookRepository, never()).save(any());
        assertThat(existing.getTotalCopies()).isEqualTo(10);
        assertThat(existing.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("update - after borrow decremented available - growing total preserves loan invariant")
    void update_growAfterBorrow() {
        existing.setTotalCopies(1);
        existing.setAvailableCopies(0);
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.update(uuid, new UpdateBookRequest(null, null, 5));

        assertThat(existing.getTotalCopies()).isEqualTo(5);
        assertThat(existing.getAvailableCopies()).isEqualTo(4);
    }

    @Test
    @DisplayName("delete - soft delete sets deletedAt and saves")
    void delete_softDelete() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        bookService.delete(uuid);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedAt()).isAfterOrEqualTo(before);
        verify(bookRepository).save(existing);
    }

    @Test
    @DisplayName("requireByUuid - returns entity when found")
    void requireByUuid_found() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        Book result = bookService.requireByUuid(uuid);

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("requireByUuid - throws when missing")
    void requireByUuid_notFound() {
        when(bookRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.requireByUuid(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }
}