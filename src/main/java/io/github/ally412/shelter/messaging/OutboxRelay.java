package io.github.ally412.shelter.messaging;

import org.springframework.transaction.annotation.Transactional;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


@Component
public class OutboxRelay {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;


    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;

    }
    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void publishEvent() {
        List<OutboxEvent> outboxEvents = outboxRepository.findUnpublished(PageRequest.of(0, 100));
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
            kafkaTemplate.send(record);
            event.setPublishedAt(Instant.now());
        }
    }

}
