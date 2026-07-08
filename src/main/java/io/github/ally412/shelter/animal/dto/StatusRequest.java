package io.github.ally412.shelter.animal.dto;

import io.github.ally412.shelter.animal.Status;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull Status status) {}
