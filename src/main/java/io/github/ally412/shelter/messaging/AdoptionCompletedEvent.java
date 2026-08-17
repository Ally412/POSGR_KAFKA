package io.github.ally412.shelter.messaging;

import java.time.LocalDate;

public record AdoptionCompletedEvent(Long animalId,
                                     String animalName,
                                     Long adopterId,
                                     String adopterName,
                                     LocalDate date) {
}
