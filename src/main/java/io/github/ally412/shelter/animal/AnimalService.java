package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalConverter;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.common.DeleteResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AnimalService {
    private final AnimalRepository animalRepository;

    public AnimalService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    public Optional<Animal> getAnimal(Long id) {
        return animalRepository.findById(id);
    }
    public List<Animal> getAnimals() {
        return animalRepository.findAll();
    }
    public Animal saveAnimal(AnimalRequest animalRequest) {
        return animalRepository.save(AnimalConverter.toNewAnimal(animalRequest));
    }

    @Transactional
    public Optional<Animal> updateAnimal(Long id, AnimalRequest animalRequest) {
        return animalRepository.findById(id)
                .map(animal -> {
                    animal.setName(animalRequest.name());
                    animal.setSpecies(animalRequest.species());
                    animal.setBreed(animalRequest.breed());
                    animal.setStatus(animalRequest.status());
                    return animal;
                });
    }

    @Transactional
    public Optional<Animal> updateStatus(Long id, Status status) {
        return animalRepository.findById(id)
                .map(animal -> {
                    animal.setStatus(status);
                    return animal;
                });
    }

    @Transactional
    public int updateStatusSocializingToAvailable() {
        return animalRepository.updateStatusSocializingToAvailable(LocalDate.now());
    }
    @Transactional
    public DeleteResult deleteAnimal(Long id) {
        if(animalRepository.existsById(id)) {
            animalRepository.deleteById(id);
            return DeleteResult.SUCCESS;
        }
        return DeleteResult.NOT_FOUND;
    }

}
