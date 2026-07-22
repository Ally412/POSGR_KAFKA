package io.github.ally412.shelter.users;

import io.github.ally412.shelter.users.dto.RegisterRequest;
import io.github.ally412.shelter.users.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

// Admin-only account management. Path is OUTSIDE /auth/** so it's covered by
// anyRequest().authenticated() (needs a valid token) AND @PreAuthorize (needs ADMIN).
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegisterResponse> createStaff(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse created = accountService.createStaff(registerRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
