package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.adoption.Adopter;
import io.github.ally412.shelter.adoption.AdopterRepository;
import io.github.ally412.shelter.adoption.dto.AdoptionRequest;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.care.Urgency;
import io.github.ally412.shelter.care.dto.MedicalRecordRequest;
import io.github.ally412.shelter.common.web.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The stage's headline flow, end to end inside the shelter: HTTP request → Postgres → outbox →
 * scheduled relay → real broker. Every other messaging test stops at one of those seams.
 * <p>
 * What this covers that they do not: the relay's eventType→topic switch. Three branches exist,
 * and until this class only "AnimalAdded" had ever run — a wrong topic on either of the others
 * would have left the whole suite green and the notifier silently starved.
 * <p>
 * Deliberately NOT @Transactional. The relay polls in its own transaction on its own thread, so
 * it can only see rows this test has committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(roles = "STAFF")
class ShelterEventFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    AdopterRepository adopterRepository;

    @Autowired
    TopicCapture capture;

    @Test
    void postingAnAnimalPublishesAnimalAdded() throws Exception {
        long animalId = createAnimal("Rex", Species.DOG, "Husky");

        ConsumerRecord<String, String> record =
                capture.await(capture.animalAdded, String.valueOf(animalId));

        JsonNode payload = objectMapper.readTree(record.value());
        assertThat(payload.get("animalId").asLong()).isEqualTo(animalId);
        assertThat(payload.get("name").asString()).isEqualTo("Rex");
        assertThat(payload.get("species").asString()).isEqualTo("DOG");
        assertCarriesFields(payload, "animalId", "name", "species", "breed", "status", "intakeDate");
        assertEventIdHeader(record);
    }

    @Test
    void postingAnUrgentMedicalRecordPublishesHealthAlert() throws Exception {
        long animalId = createAnimal("Milo", Species.CAT, "Siberian");

        MedicalRecordRequest request = new MedicalRecordRequest(
                "Limping badly", LocalDate.of(2026, 5, 1), "Dr. House", Urgency.CRITICAL);
        mockMvc.perform(post(Constants.ANIMALS + "/" + animalId + "/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ConsumerRecord<String, String> record =
                capture.await(capture.healthAlert, String.valueOf(animalId));

        JsonNode payload = objectMapper.readTree(record.value());
        assertThat(payload.get("animalId").asLong()).isEqualTo(animalId);
        assertThat(payload.get("urgency").asString()).isEqualTo("CRITICAL");
        assertThat(payload.get("description").asString()).isEqualTo("Limping badly");
        // Keyed by animal, not by medical record: one animal's events share a partition, so a
        // health alert can never overtake the AnimalAdded that created the animal.
        assertThat(record.key()).isEqualTo(String.valueOf(animalId));
        assertCarriesFields(payload, "medicalRecordId", "animalId", "animalName", "species",
                "breed", "urgency", "description", "vetName", "treatmentDate");
        assertEventIdHeader(record);
    }

    @Test
    void postingAnAdoptionPublishesAdoptionCompleted() throws Exception {
        long animalId = createAnimal("Barsik", Species.CAT, "Mix");
        Adopter adopter = new Adopter();
        adopter.setName("Maria");
        adopter.setEmail("maria@example.com");
        long adopterId = adopterRepository.save(adopter).getId();

        AdoptionRequest request = new AdoptionRequest(adopterId, LocalDate.of(2026, 8, 13));
        mockMvc.perform(post(Constants.ANIMALS + "/" + animalId + "/adoption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ConsumerRecord<String, String> record =
                capture.await(capture.adoptionCompleted, String.valueOf(animalId));

        JsonNode payload = objectMapper.readTree(record.value());
        assertThat(payload.get("animalId").asLong()).isEqualTo(animalId);
        assertThat(payload.get("adopterName").asString()).isEqualTo("Maria");
        assertThat(payload.get("date").asString()).isEqualTo("2026-08-13");
        assertCarriesFields(payload, "animalId", "animalName", "adopterId", "adopterName", "date");
        assertEventIdHeader(record);

        // The event is only half of it: the status change must have committed too, or the
        // shelter would be telling the world about an adoption its own database denies.
        assertThat(animalRepository.findById(animalId).orElseThrow().getStatus())
                .isEqualTo(Status.ADOPTED);
    }

    /**
     * The wire contract, pinned. These field names are what the notifier reads by hand — it
     * shares no code with this repo — so renaming one here silently starves it. This assertion
     * turns that into a failing build in the repo where the change was actually made.
     * <p>
     * Extra fields are fine and are not asserted against: consumers ignore what they do not
     * know, so adding a field is compatible. Removing or renaming one is not.
     */
    private void assertCarriesFields(JsonNode payload, String... required) {
        for (String field : required) {
            assertThat(payload.has(field))
                    .as("event is missing '%s' — the notifier reads this field by name", field)
                    .isTrue();
        }
    }

    /** Every event carries the outbox row id, which is what lets a consumer dedupe. */
    private void assertEventIdHeader(ConsumerRecord<String, String> record) {
        assertThat(record.headers().lastHeader("event-id"))
                .isNotNull()
                .satisfies(h -> assertThat(new String(h.value(), StandardCharsets.UTF_8))
                        .isNotBlank());
    }

    private long createAnimal(String name, Species species, String breed) throws Exception {
        String body = mockMvc.perform(post(Constants.ANIMALS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AnimalRequest(name, species, breed, Status.AVAILABLE))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TopicCapture topicCapture() {
            return new TopicCapture();
        }
    }

    /**
     * One queue per topic. These tests are not transactional, so rows from earlier tests are
     * still committed and their events keep arriving — await() therefore drains until it finds
     * the key it wants rather than trusting whatever is at the head.
     */
    static class TopicCapture {
        final BlockingQueue<ConsumerRecord<String, String>> animalAdded = new LinkedBlockingQueue<>();
        final BlockingQueue<ConsumerRecord<String, String>> healthAlert = new LinkedBlockingQueue<>();
        final BlockingQueue<ConsumerRecord<String, String>> adoptionCompleted = new LinkedBlockingQueue<>();

        @KafkaListener(topics = Topics.ANIMAL_ADDED, groupId = "shelter-event-flow-it")
        void onAnimalAdded(ConsumerRecord<String, String> record) {
            animalAdded.add(record);
        }

        @KafkaListener(topics = Topics.HEALTH_ALERT, groupId = "shelter-event-flow-it")
        void onHealthAlert(ConsumerRecord<String, String> record) {
            healthAlert.add(record);
        }

        @KafkaListener(topics = Topics.ADOPTION_COMPLETED, groupId = "shelter-event-flow-it")
        void onAdoptionCompleted(ConsumerRecord<String, String> record) {
            adoptionCompleted.add(record);
        }

        ConsumerRecord<String, String> await(BlockingQueue<ConsumerRecord<String, String>> queue,
                                             String key) throws InterruptedException {
            List<ConsumerRecord<String, String>> seen = new ArrayList<>();
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < deadline) {
                ConsumerRecord<String, String> record = queue.poll(500, TimeUnit.MILLISECONDS);
                if (record == null) {
                    continue;
                }
                if (key.equals(record.key())) {
                    return record;
                }
                seen.add(record);          // someone else's event; keep looking
            }
            throw new AssertionError("No record with key %s within 20s. Saw keys: %s"
                    .formatted(key, seen.stream().map(ConsumerRecord::key).toList()));
        }
    }
}
