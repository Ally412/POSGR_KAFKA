package io.github.ally412.shelter.adoption;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.common.web.AnimalAlreadyAdoptedException;
import io.github.ally412.shelter.common.web.NoSuchAdopterException;
import io.github.ally412.shelter.common.web.NoSuchAnimalException;
import io.github.ally412.shelter.messaging.AdoptionCompletedEvent;
import io.github.ally412.shelter.messaging.OutboxEvent;
import io.github.ally412.shelter.messaging.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * completeAdoption is three writes in one transaction: the adoption row, the animal's status,
 * and the outbox row. Postgres alone can prove all three — the broker is the relay's problem.
 */
@SpringBootTest
@Testcontainers
@Transactional
class AdoptionOutboxIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // @EnableScheduling means OutboxRelay ticks here too; mocked so it never reaches a broker.
    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    AdoptionService adoptionService;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    AdopterRepository adopterRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper;

    private Animal animal;
    private Adopter adopter;

    @BeforeEach
    void setUp() {
        // Persisted directly, not via AnimalService: saveAnimal would add an AnimalAdded row
        // and blur the outbox assertions below.
        Animal a = new Animal();
        a.setName("Rex");
        a.setSpecies(Species.DOG);
        a.setBreed("Husky");
        a.setStatus(Status.AVAILABLE);
        a.setIntakeDate(LocalDate.of(2026, 1, 15));
        animal = animalRepository.save(a);

        Adopter ad = new Adopter();
        ad.setName("Maria");
        ad.setEmail("maria@example.com");
        adopter = adopterRepository.save(ad);
    }

    @Test
    void completeAdoptionWritesAdoptionStatusAndOutboxRowTogether() {
        LocalDate date = LocalDate.of(2026, 8, 13);

        Adoption adoption = adoptionService.completeAdoption(animal.getId(), adopter.getId(), date);

        // @MapsId copies the animal's id at FLUSH, not when setAnimal is called — until then
        // adoption.getId() is null. Nothing user-facing depends on it (the response reads
        // adoption.getAnimal().getId()), but asserting it requires forcing the flush.
        animalRepository.flush();
        assertThat(adoption.getId()).isEqualTo(animal.getId());
        assertThat(adoption.getAdopter().getId()).isEqualTo(adopter.getId());
        assertThat(adoption.getDate()).isEqualTo(date);
        // The status change is the point: an adopted animal is no longer available.
        assertThat(animalRepository.findById(animal.getId()).orElseThrow().getStatus())
                .isEqualTo(Status.ADOPTED);

        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.getFirst();
        assertThat(row.getAggregateType()).isEqualTo("Animal");
        // Keyed by animal, so this shares a partition with the animal's other events.
        assertThat(row.getAggregateId()).isEqualTo(String.valueOf(animal.getId()));
        assertThat(row.getEventType()).isEqualTo("AdoptionCompleted");
        assertThat(row.getPublishedAt()).isNull();
    }

    @Test
    void outboxPayloadIsTheEventThatWillGoOnTheWire() {
        LocalDate date = LocalDate.of(2026, 8, 13);

        adoptionService.completeAdoption(animal.getId(), adopter.getId(), date);

        OutboxEvent row = outboxRepository.findAll().getFirst();

        assertThat(objectMapper.readValue(row.getPayload(), AdoptionCompletedEvent.class))
                .isEqualTo(new AdoptionCompletedEvent(
                        animal.getId(), "Rex", adopter.getId(), "Maria", date));
    }

    @Test
    void secondAdoptionOfTheSameAnimalIsRejected() {
        adoptionService.completeAdoption(animal.getId(), adopter.getId(), LocalDate.of(2026, 8, 13));

        assertThatThrownBy(() -> adoptionService.completeAdoption(
                animal.getId(), adopter.getId(), LocalDate.of(2026, 8, 14)))
                .isInstanceOf(AnimalAlreadyAdoptedException.class);
    }

    @Test
    void unknownAnimalIsRejectedBeforeAnythingIsWritten() {
        assertThatThrownBy(() -> adoptionService.completeAdoption(
                -1L, adopter.getId(), LocalDate.of(2026, 8, 13)))
                .isInstanceOf(NoSuchAnimalException.class);

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void unknownAdopterIsRejectedBeforeAnythingIsWritten() {
        assertThatThrownBy(() -> adoptionService.completeAdoption(
                animal.getId(), -1L, LocalDate.of(2026, 8, 13)))
                .isInstanceOf(NoSuchAdopterException.class);

        assertThat(outboxRepository.findAll()).isEmpty();
        assertThat(animalRepository.findById(animal.getId()).orElseThrow().getStatus())
                .isEqualTo(Status.AVAILABLE);
    }
}
