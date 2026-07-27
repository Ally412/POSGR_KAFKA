package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.common.web.NoSuchAnimalException;
import io.github.ally412.shelter.common.web.NoSuchCaretakerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimalCareService {
    private final CaretakerRepository caretakerRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AnimalRepository animalRepository;

    public AnimalCareService(CaretakerRepository caretakerRepository, MedicalRecordRepository medicalRecordRepository, AnimalRepository animalRepository) {
        this.caretakerRepository = caretakerRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.animalRepository = animalRepository;
    }

    public List<MedicalRecord> fetchMedicalRecordsByAnimalId(Long animalId) {
        return medicalRecordRepository.findByAnimalId(animalId);
    }
    public MedicalRecord addMedicalRecord(MedicalRecord medicalRecord, Long animalId) {
        if(!animalRepository.existsById(animalId)) {
            throw new NoSuchAnimalException(animalId);
        }
        Animal animal = animalRepository.getReferenceById(animalId);
        medicalRecord.setAnimal(animal);
        medicalRecord = medicalRecordRepository.save(medicalRecord);
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
