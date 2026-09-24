package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByUuid(UUID uuid);

    boolean existsByIsbn(String isbn);
}