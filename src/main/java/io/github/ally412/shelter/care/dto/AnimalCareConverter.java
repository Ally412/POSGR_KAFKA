package io.github.ally412.shelter.care.dto;

import io.github.ally412.shelter.care.Caretaker;
import io.github.ally412.shelter.care.MedicalRecord;

public class AnimalCareConverter {
    public static MedicalRecord toMedicalRecordWithoutAnimal(MedicalRecordRequest request) {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setDescription(request.description());
        medicalRecord.setTreatmentDate(request.treatmentDate());
        medicalRecord.setVetName(request.vetName());
        medicalRecord.setUrgency(request.urgency());
        return medicalRecord;
    }
    public static MedicalRecordResponse toMedicalRecordResponse(MedicalRecord medicalRecord) {
        return new MedicalRecordResponse(
                medicalRecord.getId(),
                medicalRecord.getAnimal().getId(),
                medicalRecord.getDescription(),
                medicalRecord.getTreatmentDate(),
                medicalRecord.getVetName(),
                medicalRecord.getUrgency()
        );
    }
    public static CaretakerResponse toCaretakerResponse(Caretaker caretaker) {
        return new CaretakerResponse(
                caretaker.getId(),
                caretaker.getName(),
                caretaker.getEmail(),
                caretaker.getSpecialization()
        );
    }
}
