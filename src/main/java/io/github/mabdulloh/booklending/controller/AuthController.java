package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.auth.LoginRequest;
import io.github.mabdulloh.booklending.dto.auth.LoginResponse;
import io.github.mabdulloh.booklending.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(req.username());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            throw new BadCredentialsException("invalid credentials");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("invalid credentials");
        }
        String role = user.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("MEMBER");
        return ResponseEntity.ok(new LoginResponse(jwtService.issue(user.getUsername(), role)));
    }
}