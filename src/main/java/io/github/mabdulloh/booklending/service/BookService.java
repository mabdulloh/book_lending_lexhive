package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Book;
import io.github.mabdulloh.booklending.dto.book.BookResponse;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;

import java.util.List;
import java.util.UUID;

public interface BookService {
    BookResponse create(CreateBookRequest req);
    BookResponse get(UUID uuid);
    List<BookResponse> list();
    BookResponse update(UUID uuid, UpdateBookRequest req);
    void delete(UUID uuid);
    Book requireByUuid(UUID uuid);
}