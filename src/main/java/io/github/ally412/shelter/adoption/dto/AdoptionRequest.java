package io.github.ally412.shelter.adoption.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record AdoptionRequest(@NotNull Long adopterId,
                              @NotNull @PastOrPresent LocalDate date) {
}
