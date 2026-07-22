package io.github.ally412.shelter.users;

import io.github.ally412.shelter.common.web.DuplicateCredException;
import io.github.ally412.shelter.users.dto.RegisterRequest;
import io.github.ally412.shelter.users.dto.RegisterResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Public self-registration → always USER.
    public RegisterResponse register(RegisterRequest registerRequest) {
        return create(registerRequest, Role.USER);
    }

    // Admin-only (guarded at the endpoint) → creates a STAFF account.
    public RegisterResponse createStaff(RegisterRequest registerRequest) {
        return create(registerRequest, Role.STAFF);
    }

    // Shared creation: dup-check → hash → save with the given role.
    private RegisterResponse create(RegisterRequest registerRequest, Role role) {
        accountRepository.findByUsername(registerRequest.username())
                .ifPresent(account ->
                {throw new DuplicateCredException("username", account.getUsername());});
        Account account = new Account();
        account.setUsername(registerRequest.username());
        account.setPassword(passwordEncoder.encode(registerRequest.password()));
        account.setEmail(registerRequest.email());
        account.setRole(role);
        account = accountRepository.save(account);
        return new RegisterResponse(account.getId(), account.getUsername(), account.getRole());
    }
}
