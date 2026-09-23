package io.github.mabdulloh.booklending.dto.member;

import jakarta.validation.constraints.*;

public record CreateMemberRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {}