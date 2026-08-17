package io.github.ally412.shelter.care.dto;

import io.github.ally412.shelter.care.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicalRecordRequest(@NotBlank String description,
                                   @NotNull LocalDate treatmentDate,
                                   @NotBlank String vetName,
                                   @NotNull Urgency urgency) {}
