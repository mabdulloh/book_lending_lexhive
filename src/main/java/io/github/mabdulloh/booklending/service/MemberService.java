package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.dto.member.MemberResponse;

import java.util.List;
import java.util.UUID;

public interface MemberService {
    MemberResponse create(CreateMemberRequest req);
    MemberResponse get(UUID uuid);
    List<MemberResponse> list();
    void delete(UUID uuid);
    Member requireByUuid(UUID uuid);
}