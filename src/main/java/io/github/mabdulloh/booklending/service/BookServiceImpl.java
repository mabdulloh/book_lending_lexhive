package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Book;
import io.github.mabdulloh.booklending.dto.book.BookResponse;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;
import io.github.mabdulloh.booklending.exception.DuplicateIsbnException;
import io.github.mabdulloh.booklending.exception.EntityNotFoundException;
import io.github.mabdulloh.booklending.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public BookResponse create(CreateBookRequest req) {
        log.info("Creating book isbn: {}", req.isbn());
        if (bookRepository.existsByIsbn(req.isbn())) {
            log.info("Book with isbn {} already exists", req.isbn());
            throw new DuplicateIsbnException(req.isbn());
        }
        var book = new Book();
        book.setUuid(UUID.randomUUID());
        book.setTitle(req.title());
        book.setAuthor(req.author());
        book.setIsbn(req.isbn());
        book.setTotalCopies(req.totalCopies());
        book.setAvailableCopies(req.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    @Override
    public BookResponse get(UUID uuid) {
        return BookResponse.from(findActiveByUuid(uuid));
    }

    @Override
    public List<BookResponse> list() {
        return bookRepository.findAll().stream()
                .map(BookResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BookResponse update(UUID uuid, UpdateBookRequest req) {
        Book book = findActiveByUuid(uuid);
        Optional.ofNullable(req.title()).ifPresent(book::setTitle);
        Optional.ofNullable(req.author()).ifPresent(book::setAuthor);
        if (req.totalCopies() != null) {
            int activeLoans = book.getTotalCopies() - book.getAvailableCopies();
            if (req.totalCopies() < activeLoans) {
                throw new IllegalArgumentException(
                        "totalCopies (" + req.totalCopies() + ") cannot be less than active loans (" + activeLoans + ")");
            }
            int delta = req.totalCopies() - book.getTotalCopies();
            book.setTotalCopies(req.totalCopies());
            book.setAvailableCopies(book.getAvailableCopies() + delta);
        }
        return BookResponse.from(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void delete(UUID uuid) {
        Book book = findActiveByUuid(uuid);
        book.setDeletedAt(Instant.now());
        bookRepository.save(book);
        log.info("Delete book with uuid: {}", uuid);
    }

    @Override
    public Book requireByUuid(UUID uuid) {
        return findActiveByUuid(uuid);
    }

    private Book findActiveByUuid(UUID uuid) {
        return bookRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Book", uuid));
    }
}