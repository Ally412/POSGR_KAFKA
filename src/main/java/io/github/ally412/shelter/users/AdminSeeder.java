package io.github.ally412.shelter.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial ADMIN account on startup if it doesn't already exist.
 * Admins can't self-register (register only makes USERs), so we seed one here.
 * Idempotent — safe to run on every startup. Credentials come from config/env
 * (admin.username / admin.password), so no password is committed to the repo.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public AdminSeeder(AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${admin.username:admin}") String adminUsername,
                       @Value("${admin.password:admin}") String adminPassword,
                       @Value("${admin.email:admin@shelter.io}") String adminEmail) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.findByUsername(adminUsername).isPresent()) {
            return;   // already seeded — don't create a duplicate
        }
        Account admin = new Account();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));   // hashed at runtime
        admin.setRole(Role.ADMIN);
        accountRepository.save(admin);
    }
}
