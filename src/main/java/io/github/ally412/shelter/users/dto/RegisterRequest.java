package io.github.ally412.shelter.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank String username,
                              @NotBlank String password,
                              @NotBlank @Email String email) {
}
