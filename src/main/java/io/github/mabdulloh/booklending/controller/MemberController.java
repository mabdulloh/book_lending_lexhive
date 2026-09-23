package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.dto.member.MemberResponse;
import io.github.mabdulloh.booklending.service.MemberService;
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
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberResponse> list() {
        return memberService.list();
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public MemberResponse get(@PathVariable UUID uuid) {
        return memberService.get(uuid);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        memberService.delete(uuid);
    }
}