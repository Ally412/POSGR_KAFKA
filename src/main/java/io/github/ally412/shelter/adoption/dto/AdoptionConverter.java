package io.github.ally412.shelter.adoption.dto;

import io.github.ally412.shelter.adoption.Adoption;

public final class AdoptionConverter {

    public static AdoptionResponse toAdoptionResponse(Adoption adoption) {
        return new AdoptionResponse(
                adoption.getAnimal().getId(),
                adoption.getAnimal().getName(),
                adoption.getAdopter().getId(),
                adoption.getAdopter().getName(),
                adoption.getDate());
    }

    private AdoptionConverter() {}
}
