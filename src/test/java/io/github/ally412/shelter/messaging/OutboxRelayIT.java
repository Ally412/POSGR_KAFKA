package io.github.ally412.shelter.messaging;

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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drops a row straight into the outbox and lets the scheduled relay find it — no AnimalService
 * involved, so this covers the relay's own behaviour rather than the path that feeds it.
 * <p>
 * Deliberately NOT @Transactional: the relay runs in its own transaction on its own thread and
 * would never see a row this test had not committed.
 */
@SpringBootTest
@Testcontainers
class OutboxRelayIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PublishedEvents published;

    @Test
    void relayShipsTheStoredPayloadAndMarksItPublished() throws Exception {
        // Nonsense to the domain, valid to the relay: it moves bytes and never parses them,
        // which is what lets old rows stay publishable after the event's shape changes.
        String payload = "{\"animalId\":42,\"whatever\":\"the relay does not care\"}";
        OutboxEvent row = new OutboxEvent();
        row.setAggregateType("Animal");
        row.setAggregateId("42");
        row.setEventType("AnimalAdded");
        row.setPayload(payload);
        OutboxEvent stored = outboxRepository.save(row);

        ConsumerRecord<String, String> record = published.queue.poll(15, TimeUnit.SECONDS);

        assertThat(record).as("relay should have published within 15s").isNotNull();
        assertThat(record.key()).isEqualTo("42");
        // Equivalent JSON, not identical bytes: jsonb parses on write and re-renders on read,
        // so spacing is normalised and key order is not guaranteed. (Plain `json` would keep
        // the original text.) What must survive the round trip is the content, not the layout.
        assertThat(objectMapper.readTree(record.value())).isEqualTo(objectMapper.readTree(payload));
        // Carries the outbox row id so a consumer can recognise a redelivery of the same event.
        assertThat(record.headers().lastHeader("event-id"))
                .isNotNull()
                .satisfies(header -> assertThat(new String(header.value(), StandardCharsets.UTF_8))
                        .isEqualTo(String.valueOf(stored.getId())));

        assertThat(awaitPublishedAt(stored.getId()))
                .as("row should be marked published so the next tick skips it")
                .isNotNull();
    }

    /** The relay stamps publishedAt in its own transaction, which commits after the send. */
    private Instant awaitPublishedAt(Long id) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            Instant publishedAt = outboxRepository.findById(id).orElseThrow().getPublishedAt();
            if (publishedAt != null) {
                return publishedAt;
            }
            Thread.sleep(200);
        }
        return null;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PublishedEvents publishedEvents() {
            return new PublishedEvents();
        }
    }

    static class PublishedEvents {
        final BlockingQueue<ConsumerRecord<String, String>> queue = new LinkedBlockingQueue<>();

        @KafkaListener(topics = Topics.ANIMAL_ADDED, groupId = "outbox-relay-it")
        void receive(ConsumerRecord<String, String> record) {
            queue.add(record);
        }
    }
}
