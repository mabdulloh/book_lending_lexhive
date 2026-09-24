package io.github.mabdulloh.booklending.service;

import io.github.mabdulloh.booklending.domain.Member;
import io.github.mabdulloh.booklending.domain.User;
import io.github.mabdulloh.booklending.dto.member.CreateMemberRequest;
import io.github.mabdulloh.booklending.exception.ConflictException;
import io.github.mabdulloh.booklending.exception.EntityNotFoundException;
import io.github.mabdulloh.booklending.repository.MemberRepository;
import io.github.mabdulloh.booklending.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private MemberServiceImpl memberService;

    private UUID uuid;
    private Member existing;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(memberRepository, userRepository, passwordEncoder);
        uuid = UUID.randomUUID();
        existing = new Member();
        existing.setUuid(uuid);
        existing.setName("Alice");
        existing.setEmail("alice@example.com");
    }

    @Test
    @DisplayName("create - success - member and user linked via memberId")
    void create_success() {
        when(memberRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.findByUsername("bob@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(42L);
            return m;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = memberService.create(new CreateMemberRequest("Bob", "bob@example.com", "secret123"));

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getEmail()).isEqualTo("bob@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("bob@example.com");
        assertThat(savedUser.getRole()).isEqualTo("MEMBER");
        assertThat(savedUser.getMember().getId()).isEqualTo(42L);
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed");

        assertThat(resp.email()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("create - duplicate email throws ConflictException")
    void create_duplicateEmail() {
        when(memberRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.create(new CreateMemberRequest("Alice", "alice@example.com", "secret123")))
                .isInstanceOf(ConflictException.class);

        verify(memberRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("get - returns response when found")
    void get_found() {
        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        var resp = memberService.get(uuid);

        assertThat(resp.uuid()).isEqualTo(uuid);
        assertThat(resp.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("get - throws EntityNotFoundException when missing")
    void get_notFound() {
        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.get(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("list - returns mapped responses")
    void list_ok() {
        when(memberRepository.findAll()).thenReturn(List.of(existing));

        var list = memberService.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).uuid()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("delete - soft delete sets deletedAt on member")
    void delete_softDelete() {
        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        memberService.delete(uuid);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedAt()).isAfterOrEqualTo(before);
        verify(memberRepository, times(1)).save(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - also soft-deletes linked user when present")
    void delete_alsoSoftDeletesLinkedUser() {
        existing.setId(42L);
        User linkedUser = new User();
        linkedUser.setId(99L);
        linkedUser.setPasswordHash("hash");

        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByMemberId(42L)).thenReturn(Optional.of(linkedUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.delete(uuid);

        assertThat(linkedUser.getDeletedAt()).isNotNull();
        verify(userRepository, times(1)).save(linkedUser);
    }

    @Test
    @DisplayName("requireByUuid - returns entity when found")
    void requireByUuid_found() {
        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));

        Member result = memberService.requireByUuid(uuid);

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("requireByUuid - throws when missing")
    void requireByUuid_notFound() {
        when(memberRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.requireByUuid(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }
}