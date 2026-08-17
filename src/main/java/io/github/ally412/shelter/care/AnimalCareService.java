package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.common.web.NoSuchAnimalException;
import io.github.ally412.shelter.common.web.NoSuchCaretakerException;
import io.github.ally412.shelter.messaging.HealthAlertEvent;
import io.github.ally412.shelter.messaging.OutboxEvent;
import io.github.ally412.shelter.messaging.OutboxRepository;
import io.github.ally412.shelter.messaging.Topics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class AnimalCareService {
    private final CaretakerRepository caretakerRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AnimalRepository animalRepository;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public AnimalCareService(CaretakerRepository caretakerRepository,
                             MedicalRecordRepository medicalRecordRepository,
                             AnimalRepository animalRepository,
                             ObjectMapper objectMapper,
                             OutboxRepository outboxRepository) {
        this.caretakerRepository = caretakerRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.animalRepository = animalRepository;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    public List<MedicalRecord> fetchMedicalRecordsByAnimalId(Long animalId) {
        return medicalRecordRepository.findByAnimalId(animalId);
    }
    @Transactional
    public MedicalRecord addMedicalRecord(MedicalRecord medicalRecord, Long animalId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new NoSuchAnimalException(animalId));
        medicalRecord.setAnimal(animal);
        medicalRecord = medicalRecordRepository.save(medicalRecord);
        if(medicalRecord.getUrgency() != Urgency.ROUTINE) {
            HealthAlertEvent event = new HealthAlertEvent(
                    medicalRecord.getId(),
                    animal.getId(),
                    animal.getName(),
                    animal.getSpecies(),
                    animal.getBreed(),
                    medicalRecord.getUrgency(),
                    medicalRecord.getDescription(),
                    medicalRecord.getVetName(),
                    medicalRecord.getTreatmentDate()
            );
            OutboxEvent outboxEvent = new OutboxEvent();
            // Keyed by animal, not by medical record: one animal's events share a partition
            // and stay ordered, so an alert cannot overtake its own AnimalAdded.
            outboxEvent.setAggregateId(String.valueOf(animalId));
            outboxEvent.setAggregateType("Animal");
            outboxEvent.setEventType("HealthAlert");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(outboxEvent);
        }
        return medicalRecord;
    }
    public List<Caretaker> fetchCareTakersByAnimalId(Long animalId) {
        return caretakerRepository.findByAnimalId(animalId);
    }

    @Transactional
    public void assignCareTakerToAnimal(Long animalId, Long careTakerId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new NoSuchAnimalException(animalId));
        if(!caretakerRepository.existsById(careTakerId)) {
            throw new NoSuchCaretakerException(careTakerId);
        }
        Caretaker caretaker = caretakerRepository.getReferenceById(careTakerId);
        animal.getCaretakers().add(caretaker);   // owning side; @Transactional → dirty check writes the join row
    }

}
