package io.github.ally412.shelter.users.dto;

import io.github.ally412.shelter.users.Role;

public record RegisterResponse(Long id, String username, Role role) {
}
