package io.github.ally412.shelter.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Value("${shelter.kafka.replicas:1}")
    private short replicas;
    @Bean
    public NewTopic animalAddedTopic() {
        return topic(Topics.ANIMAL_ADDED);
    }
    @Bean
    public NewTopic adoptionCompletedTopic() {
        return topic(Topics.ADOPTION_COMPLETED);
    }
    @Bean
    public NewTopic healthAlertTopic() {
        return topic(Topics.HEALTH_ALERT);
    }
    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(replicas).build();
    }
}
