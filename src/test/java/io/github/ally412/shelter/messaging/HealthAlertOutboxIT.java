package io.github.ally412.shelter.messaging;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalService;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.care.AnimalCareService;
import io.github.ally412.shelter.care.MedicalRecord;
import io.github.ally412.shelter.care.Urgency;
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

/**
 * Only a serious medical record is an alert. Proving that needs no broker — the decision is
 * visible as the presence or absence of an outbox row.
 */
@SpringBootTest
@Testcontainers
@Transactional
class HealthAlertOutboxIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    AnimalService animalService;

    @Autowired
    AnimalCareService animalCareService;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void urgentMedicalRecordRaisesAHealthAlert() {
        Animal animal = givenAnimal();

        MedicalRecord record = animalCareService.addMedicalRecord(
                medicalRecord("Limping badly", "Dr. House", Urgency.URGENT), animal.getId());

        OutboxEvent row = onlyHealthAlertRow();
        assertThat(row.getAggregateType()).isEqualTo("Animal");
        // keyed by animal so alerts stay ordered behind that animal's other events
        assertThat(row.getAggregateId()).isEqualTo(String.valueOf(animal.getId()));

        assertThat(objectMapper.readValue(row.getPayload(), HealthAlertEvent.class))
                .isEqualTo(new HealthAlertEvent(
                        record.getId(),
                        animal.getId(),
                        "Rex",
                        Species.DOG,
                        "Husky",
                        Urgency.URGENT,
                        "Limping badly",
                        "Dr. House",
                        record.getTreatmentDate()));
    }

    @Test
    void criticalMedicalRecordRaisesAHealthAlert() {
        Animal animal = givenAnimal();

        animalCareService.addMedicalRecord(
                medicalRecord("Collapsed", "Dr. House", Urgency.CRITICAL), animal.getId());

        assertThat(onlyHealthAlertRow().getEventType()).isEqualTo("HealthAlert");
    }

    /** The conditional publish is the whole feature; without this it can invert unnoticed. */
    @Test
    void routineMedicalRecordRaisesNothing() {
        Animal animal = givenAnimal();

        animalCareService.addMedicalRecord(
                medicalRecord("Annual check-up", "Dr. House", Urgency.ROUTINE), animal.getId());

        assertThat(healthAlertRows()).isEmpty();
    }

    private Animal givenAnimal() {
        return animalService.saveAnimal(new AnimalRequest("Rex", Species.DOG, "Husky", Status.AVAILABLE));
    }

    private MedicalRecord medicalRecord(String description, String vetName, Urgency urgency) {
        MedicalRecord record = new MedicalRecord();
        record.setDescription(description);
        record.setVetName(vetName);
        record.setTreatmentDate(LocalDate.now());
        record.setUrgency(urgency);
        return record;
    }

    /** saveAnimal writes an AnimalAdded row of its own, so filter rather than counting everything. */
    private List<OutboxEvent> healthAlertRows() {
        return outboxRepository.findAll().stream()
                .filter(row -> "HealthAlert".equals(row.getEventType()))
                .toList();
    }

    private OutboxEvent onlyHealthAlertRow() {
        List<OutboxEvent> rows = healthAlertRows();
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }
}
