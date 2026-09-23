package io.github.mabdulloh.booklending.repository;

import io.github.mabdulloh.booklending.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("select b from Book b where b.uuid = :uuid and b.deletedAt is null")
    Optional<Book> findByUuid(UUID uuid);

    @Query("select b from Book b where b.deletedAt is null")
    List<Book> findAllActive();

    @Query("select b from Book b where b.uuid = :uuid and b.deletedAt is not null")
    Optional<Book> findDeletedByUuid(UUID uuid);

    boolean existsByIsbnAndDeletedAtIsNull(String isbn);
}