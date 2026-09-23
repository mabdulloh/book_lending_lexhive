package io.github.mabdulloh.booklending.controller;

import io.github.mabdulloh.booklending.dto.auth.LoginRequest;
import io.github.mabdulloh.booklending.dto.auth.LoginResponse;
import io.github.mabdulloh.booklending.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "1. Auth", description = "Authentication endpoints")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Login with username + password for a signed JWT. Use the token in `Authorization: Bearer ...` for subsequent requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = "{\"token\": \"eyJhbGciOiJIUzI1NiJ9...\"}"))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(examples = @ExampleObject(value = "{\"timestamp\": \"...\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"invalid credentials\"}")))
    })
    public ResponseEntity<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = "{\"username\": \"admin\", \"password\": \"admin123\"}"))
            )
            @RequestBody LoginRequest req) {
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