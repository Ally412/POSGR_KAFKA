package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;

import java.time.LocalDate;


public record AnimalAddedEvent(Long animalId,
                               String name,
                               Species species,
                               String breed,
                               Status status,
                               LocalDate intakeDate) {
}
