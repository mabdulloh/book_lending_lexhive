package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.book.BookResponse;
import io.github.mabdulloh.booklending.dto.book.CreateBookRequest;
import io.github.mabdulloh.booklending.dto.book.UpdateBookRequest;
import io.github.mabdulloh.booklending.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "2. Books", description = "Catalog management for books")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new book",
            description = "Admin-only. Adds a book to the catalog. `availableCopies` defaults to `totalCopies`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created",
                    content = @Content(schema = @Schema(implementation = BookResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "title": "Clean Code",
                                      "author": "Robert C. Martin",
                                      "isbn": "9780132350884",
                                      "totalCopies": 5,
                                      "availableCopies": 5,
                                      "createdAt": "2026-09-23T13:00:00Z",
                                      "updatedAt": "2026-09-23T13:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "ISBN already exists"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role")
    })
    public ResponseEntity<BookResponse> create(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Book payload",
                    schema = @Schema(implementation = CreateBookRequest.class),
                    example = """
                            {
                              "title": "Clean Code",
                              "author": "Robert C. Martin",
                              "isbn": "9780132350884",
                              "totalCopies": 5
                            }""")
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateBookRequest req) {
        BookResponse created = bookService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "List all active books",
            description = "Returns non-Deleted books.")
    @ApiResponse(responseCode = "200", description = "List of books",
            content = @Content(schema = @Schema(implementation = BookResponse.class)))
    public List<BookResponse> list() {
        return bookService.list();
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Get book by UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found or Deleted")
    })
    public BookResponse get(
            @io.swagger.v3.oas.annotations.Parameter(description = "Book UUID") @PathVariable UUID uuid) {
        return bookService.get(uuid);
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update book",
            description = "All fields optional. Adjusts `availableCopies` based on `totalCopies` delta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public BookResponse update(
            @io.swagger.v3.oas.annotations.Parameter(description = "Book UUID") @PathVariable UUID uuid,
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Update payload. Null fields are skipped.",
                    schema = @Schema(implementation = UpdateBookRequest.class),
                    example = """
                            {
                              "title": "Clean Code (2nd ed.)",
                              "author": null,
                              "totalCopies": 7
                            }""")
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateBookRequest req) {
        return bookService.update(uuid, req);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a book",
            description = "Sets `deletedAt`. Book no longer appears in list/get.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @io.swagger.v3.oas.annotations.Parameter(description = "Book UUID") @PathVariable UUID uuid) {
        bookService.delete(uuid);
    }
}