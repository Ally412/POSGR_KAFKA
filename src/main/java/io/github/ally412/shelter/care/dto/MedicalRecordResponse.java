package io.github.ally412.shelter.care.dto;

import java.time.LocalDate;

public record MedicalRecordResponse(Long id,
                                    Long animalId,
                                    String description,
                                    LocalDate treatmentDate,
                                    String vetName) {}
