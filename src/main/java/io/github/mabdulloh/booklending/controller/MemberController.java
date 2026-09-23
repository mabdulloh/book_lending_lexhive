package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.dto.member.MemberResponse;
import io.github.mabdulloh.booklending.service.MemberService;
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
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "3. Members", description = "Library member management")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new member",
            description = "Admin-only. Also creates a linked `User` (role `MEMBER`) so the member can log in with the supplied password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member created",
                    content = @Content(schema = @Schema(implementation = MemberResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "uuid": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "name": "Jane Doe",
                                      "email": "jane@example.com",
                                      "createdAt": "2026-09-23T13:00:00Z",
                                      "updatedAt": "2026-09-23T13:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Duplicate email")
    })
    public ResponseEntity<MemberResponse> create(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Member + initial credentials",
                    schema = @Schema(implementation = CreateMemberRequest.class),
                    example = """
                            {
                              "name": "Jane Doe",
                              "email": "jane@example.com",
                              "password": "secret123"
                            }""")
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all active members")
    @ApiResponse(responseCode = "200", description = "List of members",
            content = @Content(schema = @Schema(implementation = MemberResponse.class)))
    public List<MemberResponse> list() {
        return memberService.list();
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Get member by UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found",
                    content = @Content(schema = @Schema(implementation = MemberResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public MemberResponse get(
            @io.swagger.v3.oas.annotations.Parameter(description = "Member UUID") @PathVariable UUID uuid) {
        return memberService.get(uuid);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a member")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted (and linked user too)"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @io.swagger.v3.oas.annotations.Parameter(description = "Member UUID") @PathVariable UUID uuid) {
        memberService.delete(uuid);
    }
}