package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalService;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The point of the outbox: saveAnimal's promise to publish is now a row in the database, so
 * proving it needs no broker at all — only Postgres. Contrast AnimalEventPublishingIT, which
 * runs the whole path through a real Kafka.
 */
@SpringBootTest
@Testcontainers
@Transactional
class AnimalOutboxIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // OutboxRelay needs a template, and @EnableScheduling means it ticks during this test too.
    // Mocked so nothing reaches for a broker that isn't here.
    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    AnimalService animalService;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void saveAnimalRecordsTheEventInTheSameTransaction() {
        Animal saved = animalService.saveAnimal(
                new AnimalRequest("Rex", Species.DOG, "Husky", Status.AVAILABLE));

        List<OutboxEvent> rows = outboxRepository.findAll();

        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.getFirst();
        assertThat(row.getAggregateType()).isEqualTo("Animal");
        assertThat(row.getAggregateId()).isEqualTo(String.valueOf(saved.getId()));
        // The event type is a domain fact, deliberately not a topic name — the relay owns that mapping.
        assertThat(row.getEventType()).isEqualTo("AnimalAdded");
        assertThat(row.getCreatedAt()).isNotNull();
        // Nothing has shipped it yet; that is the relay's job, in its own transaction.
        assertThat(row.getPublishedAt()).isNull();
    }

    @Test
    void outboxPayloadIsTheEventThatWillGoOnTheWire() {
        Animal saved = animalService.saveAnimal(
                new AnimalRequest("Barsik", Species.CAT, "Siberian", Status.SOCIALIZING));

        OutboxEvent row = outboxRepository.findAll().getFirst();

        assertThat(objectMapper.readValue(row.getPayload(), AnimalAddedEvent.class))
                .isEqualTo(new AnimalAddedEvent(saved.getId(), "Barsik", Species.CAT, "Siberian",
                        saved.getStatus(), saved.getIntakeDate()));
    }
}
