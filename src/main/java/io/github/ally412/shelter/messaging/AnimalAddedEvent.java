package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Species;

public record AnimalAddedEvent(Long animalId, String name, Species species, String breed) {
}
