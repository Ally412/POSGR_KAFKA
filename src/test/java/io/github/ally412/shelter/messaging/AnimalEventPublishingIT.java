package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalService;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves what saveAnimal actually puts on the broker: a real producer, a real broker,
 * and the exact bytes a separate consumer app would receive.
 */
@SpringBootTest
@Testcontainers
class AnimalEventPublishingIT {

    // Full app context boots → still needs a database.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // @ServiceConnection wires spring.kafka.bootstrap-servers to this broker.
    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @Autowired
    AnimalService animalService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EventCapture capture;

    @Test
    void saveAnimalPublishesEventKeyedByAnimalId() throws Exception {
        Animal saved = animalService.saveAnimal(
                new AnimalRequest("Rex", Species.DOG, "Husky", Status.AVAILABLE));

        ConsumerRecord<String, String> record = awaitRecordWithKey(String.valueOf(saved.getId()));

        // The key decides the partition: every event for one animal lands on the same
        // partition, so per-animal ordering holds even though the topic has three.
        assertThat(record.key()).isEqualTo(String.valueOf(saved.getId()));
        assertThat(objectMapper.readValue(record.value(), AnimalAddedEvent.class))
                .isEqualTo(new AnimalAddedEvent(saved.getId(), "Rex", Species.DOG, "Husky",
                        // read back from the entity: intakeDate is stamped server-side with
                        // LocalDate.now(), so a literal date would start failing tomorrow
                        saved.getStatus(), saved.getIntakeDate()));
    }

    @Test
    void eventCarriesPlainJsonAndNoProducerClassName() throws Exception {
        Animal saved = animalService.saveAnimal(
                new AnimalRequest("Barsik", Species.CAT, "Siberian", Status.SOCIALIZING));

        ConsumerRecord<String, String> record = awaitRecordWithKey(String.valueOf(saved.getId()));

        // spring.json.add.type.headers=false — a foreign app must be able to read this
        // without any of our classes, so nothing about our package layout may leak out.
        assertThat(record.headers().lastHeader("__TypeId__")).isNull();
        assertThat(record.value()).doesNotContain("io.github.ally412");
    }

    /**
     * Polls past events left by other tests until the one we just published shows up.
     * Keying by animal id is what makes that possible without isolating the topic.
     */
    private ConsumerRecord<String, String> awaitRecordWithKey(String key) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            ConsumerRecord<String, String> record = capture.queue.poll(1, TimeUnit.SECONDS);
            if (record != null && key.equals(record.key())) {
                return record;
            }
        }
        throw new AssertionError("No event with key " + key + " arrived within 15s");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        EventCapture eventCapture() {
            return new EventCapture();
        }
    }

    static class EventCapture {
        final BlockingQueue<ConsumerRecord<String, String>> queue = new LinkedBlockingQueue<>();

        // Reads with the app's own StringDeserializer, so `value` is the raw JSON text
        // that crossed the wire — not something Jackson already turned back into an object.
        @KafkaListener(topics = Topics.ANIMAL_ADDED, groupId = "animal-event-publishing-it")
        void receive(ConsumerRecord<String, String> record) {
            queue.add(record);
        }
    }
}
