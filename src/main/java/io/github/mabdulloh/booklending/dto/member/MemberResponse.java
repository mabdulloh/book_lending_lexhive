package io.github.mabdulloh.booklending.dto.member;

import io.github.mabdulloh.booklending.domain.Member;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID uuid,
        String name,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(
                m.getUuid(),
                m.getName(),
                m.getEmail(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}