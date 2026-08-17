package io.github.ally412.shelter.adoption.dto;

import java.time.LocalDate;

public record AdoptionResponse(Long animalId,
                               String animalName,
                               Long adopterId,
                               String adopterName,
                               LocalDate date) {
}
