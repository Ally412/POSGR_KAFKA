package io.github.ally412.shelter.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class KafkaConnectivityIT {

    static final String TOPIC = "connectivity-test";

    // Full app context boots → still needs a database.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // @ServiceConnection wires spring.kafka.bootstrap-servers to this broker.
    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    TestListener listener;

    @Test
    void messageRoundTripsThroughBroker() throws Exception {
        kafkaTemplate.send(TOPIC, "hello-kafka");

        String received = listener.queue.poll(10, TimeUnit.SECONDS);
        assertThat(received).isEqualTo("hello-kafka");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        NewTopic connectivityTopic() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }

        @Bean
        TestListener testListener() {
            return new TestListener();
        }
    }

    static class TestListener {
        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC, groupId = "connectivity-it")
        void receive(String message) {
            queue.add(message);
        }
    }
}
