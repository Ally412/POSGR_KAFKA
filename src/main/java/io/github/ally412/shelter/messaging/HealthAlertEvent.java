package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.care.Urgency;

import java.time.LocalDate;

public record HealthAlertEvent(Long medicalRecordId,
                               Long animalId,
                               String animalName,
                               Species species,
                               String breed,
                               Urgency urgency,
                               String description,
                               String vetName,
                               LocalDate treatmentDate) {
}
