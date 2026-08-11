package io.github.ally412.shelter.messaging;

import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.annotation.Transactional;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;


@Component
public class OutboxRelay {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    // Advisory-lock key: whoever holds it is the single relay running this tick, which is what
    // keeps one aggregate's events in order across replicas. The number is only a name, and the
    // key space is global to the database — never reuse it for another lock.
    private static final long OUTBOX_RELAY_LOCK = 774_100_001L;


    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;

    }
    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void publishEvent() {
        // The guard must be inside the @Transactional method. The lock is
        //  transaction-scoped, so if it's taken outside a transaction Spring
        //  wraps it in its own tiny one that commits immediately, releasing
        //  the lock before any work happens.
        if (!outboxRepository.tryClaimRelay(OUTBOX_RELAY_LOCK)) {
            return;
        }
        List<OutboxEvent> outboxEvents = outboxRepository.findUnpublished(PageRequest.of(0, 100));
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>();
        for (OutboxEvent event : outboxEvents) {
            String topic = switch (event.getEventType()) {
                case "AnimalAdded" -> Topics.ANIMAL_ADDED;
                default -> throw new IllegalStateException("No topic mapped for event type " + event.getEventType());
            };
            ProducerRecord<String, String> record = new
                    ProducerRecord<>(topic, event.getAggregateId(),
                    event.getPayload());
            record.headers().add("event-id",
                    String.valueOf(event.getId()).getBytes(UTF_8));
            futures.add(kafkaTemplate.send(record));
            event.setPublishedAt(Instant.now());
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}
