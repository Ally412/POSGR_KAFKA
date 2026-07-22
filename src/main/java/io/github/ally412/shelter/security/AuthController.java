package io.github.ally412.shelter.security;


import io.github.ally412.shelter.security.dto.LoginRequest;
import io.github.ally412.shelter.security.dto.TokenResponse;
import io.github.ally412.shelter.users.AccountRepository;
import io.github.ally412.shelter.users.AccountService;
import io.github.ally412.shelter.users.dto.RegisterRequest;
import io.github.ally412.shelter.users.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AccountService accountService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService, AccountRepository accountRepository, AccountService accountService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public TokenResponse login (@RequestBody LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        return new TokenResponse(tokenService.issueToken(auth));
    }
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registered = accountService.register(registerRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registered.id())
                .toUri();
        return ResponseEntity
                .created(location)
                .body(registered);
    }
}
