package io.github.ally412.shelter.adoption;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.common.web.AnimalAlreadyAdoptedException;
import io.github.ally412.shelter.common.web.NoSuchAdopterException;
import io.github.ally412.shelter.common.web.NoSuchAnimalException;
import io.github.ally412.shelter.messaging.AdoptionCompletedEvent;
import io.github.ally412.shelter.messaging.OutboxEvent;
import io.github.ally412.shelter.messaging.OutboxRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

@Service
public class AdoptionService {

    private final AnimalRepository animalRepository;
    private final AdopterRepository adopterRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AdoptionService(AnimalRepository animalRepository,
                           AdopterRepository adopterRepository,
                           OutboxRepository outboxRepository,
                           ObjectMapper objectMapper) {
        this.animalRepository = animalRepository;
        this.adopterRepository = adopterRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(value = "animals", key = "#animalId")
    public Adoption completeAdoption(Long animalId, Long adopterId, LocalDate date) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new NoSuchAnimalException(animalId));
        // @MapsId makes adoption.id the animal's id, so a second adoption would collide on
        // the primary key. Fail with something meaningful instead.
        if (animal.getAdoption() != null) {
            throw new AnimalAlreadyAdoptedException(animalId);
        }
        Adopter adopter = adopterRepository.findById(adopterId)
                .orElseThrow(() -> new NoSuchAdopterException(adopterId));

        Adoption adoption = new Adoption();
        adoption.setAnimal(animal);      // owning side: supplies the @MapsId id
        adoption.setAdopter(adopter);
        adoption.setDate(date);
        animal.setAdoption(adoption);    // inverse side: the path the cascade follows
        animal.setStatus(Status.ADOPTED);

        AdoptionCompletedEvent event = new AdoptionCompletedEvent(
                animal.getId(),
                animal.getName(),
                adopter.getId(),
                adopter.getName(),
                date);

        OutboxEvent outboxEvent = new OutboxEvent();
        // Keyed by animal so this shares a partition with the animal's other events
        // and cannot overtake the AnimalAdded that created it.
        outboxEvent.setAggregateId(String.valueOf(animalId));
        outboxEvent.setAggregateType("Animal");
        outboxEvent.setEventType("AdoptionCompleted");
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxRepository.save(outboxEvent);

        return adoption;
    }
}
