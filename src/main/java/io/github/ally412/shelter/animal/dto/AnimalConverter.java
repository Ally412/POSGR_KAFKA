package io.github.ally412.shelter.animal.dto;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.Status;

import java.time.LocalDate;

public final class AnimalConverter {
    private AnimalConverter() {}
    public static AnimalResponse toAnimalResponse(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getName(),
                animal.getSpecies(),
                animal.getBreed(),
                animal.getIntakeDate(),
                animal.getStatus()
        );
    }
    public static Animal toNewAnimal(AnimalRequest request) {
        Animal animal = new Animal();
        animal.setName(request.name());
        animal.setSpecies(request.species());
        animal.setBreed(request.breed());
        animal.setStatus(request.status());
        animal.setIntakeDate(LocalDate.now());
        return animal;
    }
}
