package io.github.ally412.shelter.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "outbox")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String aggregateId;
    @NotBlank
    private String aggregateType;
    @NotBlank
    private String eventType;
    @NotNull
    private Instant createdAt = Instant.now();
    private Instant publishedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    private  String payload;
}
