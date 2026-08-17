package io.github.ally412.shelter.care.dto;

import io.github.ally412.shelter.care.Urgency;

import java.time.LocalDate;

public record MedicalRecordResponse(Long id,
                                    Long animalId,
                                    String description,
                                    LocalDate treatmentDate,
                                    String vetName,
                                    Urgency urgency) {}
