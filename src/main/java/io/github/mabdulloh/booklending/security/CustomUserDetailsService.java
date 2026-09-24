package io.github.mabdulloh.booklending.security;

import io.github.mabdulloh.booklending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private static final String ROLE = "ROLE_";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(ROLE + user.getRole())))
                .build();
    }
}