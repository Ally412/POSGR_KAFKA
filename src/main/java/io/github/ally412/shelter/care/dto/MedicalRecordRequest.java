package io.github.ally412.shelter.care.dto;

import io.github.ally412.shelter.animal.Animal;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicalRecordRequest(@NotBlank String description,
                                   @NotNull LocalDate treatmentDate,
                                   @NotBlank String vetName) {}
