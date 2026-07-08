package io.github.ally412.shelter.animal.dto;

import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnimalRequest(@NotBlank String name,
                           @NotNull Species species,
                           @NotBlank String breed,
                           @NotNull Status status) {}
