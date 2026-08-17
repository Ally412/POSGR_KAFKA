package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalConverter;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.animal.dto.AnimalSearchCriteria;
import io.github.ally412.shelter.common.DeleteResult;
import io.github.ally412.shelter.common.web.Constants;
import io.github.ally412.shelter.common.web.IllegalStatusTransitionException;
import io.github.ally412.shelter.messaging.AnimalAddedEvent;
import io.github.ally412.shelter.messaging.OutboxEvent;
import io.github.ally412.shelter.messaging.OutboxRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AnimalService {
    private final AnimalRepository animalRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AnimalService(AnimalRepository animalRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.animalRepository = animalRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "animals", unless = "#result == null")
    public Optional<Animal> getAnimal(Long id) {
        return animalRepository.findById(id);
    }
    public List<Animal> getAnimals() {
        return animalRepository.findAll();
    }
    @Transactional
    public Animal saveAnimal(AnimalRequest animalRequest) {
        Animal saved = animalRepository.save(AnimalConverter.toNewAnimal(animalRequest));
        AnimalAddedEvent event = new AnimalAddedEvent(
                saved.getId(),
                saved.getName(),
                saved.getSpecies(),
                saved.getBreed(),
                saved.getStatus(),
                saved.getIntakeDate());
        String eventPayload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("Animal");
        outboxEvent.setAggregateId(String.valueOf(saved.getId()));
        outboxEvent.setEventType("AnimalAdded");
        outboxEvent.setPayload(eventPayload);
        outboxRepository.save(outboxEvent);
        return saved;
    }

    @CacheEvict(value = "animals", key = "#id")
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

    @CacheEvict(value = "animals", key = "#id")
    @Transactional
    public Optional<Animal> updateStatus(Long id, Status status) {
        // ADOPTED is not a status you set — it is the outcome of recording an adoption,
        // which also writes the adoption row and publishes the event.
        if (status == Status.ADOPTED) {
            throw new IllegalStatusTransitionException(
                    "Use POST " + Constants.ANIMALS + "/{id}/adoption to record an adoption.");
        }
        return animalRepository.findById(id)
                .map(animal -> {
                    animal.setStatus(status);
                    return animal;
                });
    }

    @CacheEvict(value = "animals", allEntries = true)
    @Transactional
    public int updateStatusSocializingToAvailable() {
        return animalRepository.updateStatusSocializingToAvailable(LocalDate.now());
    }
    @CacheEvict(value = "animals", key = "#id")
    @Transactional
    public DeleteResult deleteAnimal(Long id) {
        if(animalRepository.existsById(id)) {
            animalRepository.deleteById(id);
            return DeleteResult.SUCCESS;
        }
        return DeleteResult.NOT_FOUND;
    }

    public Page<Animal> search(AnimalSearchCriteria c, Pageable pageable) {
        Specification<Animal> spec = Specification.allOf();
        if(c.nameFragment() != null) spec = spec.and(AnimalSpecifications.nameContains(c.nameFragment()));
        if(c.species() != null) spec = spec.and(AnimalSpecifications.hasSpecies(c.species()));
        if(c.status() != null) spec = spec.and(AnimalSpecifications.hasStatus(c.status()));
        if(c.intakeFrom() != null) spec = spec.and(AnimalSpecifications.intakeFrom(c.intakeFrom()));
        if(c.intakeTo() != null) spec = spec.and(AnimalSpecifications.intakeTo(c.intakeTo()));
        return animalRepository.findAll(spec, pageable);
    }

}
