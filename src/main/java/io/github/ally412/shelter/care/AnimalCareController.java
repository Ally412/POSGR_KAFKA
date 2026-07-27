package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.dto.AnimalConverter;
import io.github.ally412.shelter.care.dto.AnimalCareConverter;
import io.github.ally412.shelter.care.dto.CaretakerResponse;
import io.github.ally412.shelter.care.dto.MedicalRecordRequest;
import io.github.ally412.shelter.care.dto.MedicalRecordResponse;
import io.github.ally412.shelter.common.web.Constants;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(Constants.ANIMALS)
public class AnimalCareController {
    private final AnimalCareService service;

    public AnimalCareController(AnimalCareService service) {
        this.service = service;
    }
    @GetMapping("/{animalId}/medical-records")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<MedicalRecordResponse>> fetchMedicalRecordsByAnimalId(@PathVariable Long animalId) {
        return ResponseEntity.ok(service.fetchMedicalRecordsByAnimalId(animalId).stream()
                .map(AnimalCareConverter::toMedicalRecordResponse)
                .toList());
    }
    @PostMapping("/{animalId}/medical-records")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<MedicalRecordResponse> addMedicalRecord (
            @Valid @RequestBody MedicalRecordRequest medicalRecordRequest,
            @PathVariable Long animalId) {
        MedicalRecord saved = service.addMedicalRecord(
                AnimalCareConverter.toMedicalRecordWithoutAnimal(medicalRecordRequest),
                animalId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(Constants.ANIMALS + "/{animalId}/medical-records/{recordId}")
                .buildAndExpand(animalId, saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(AnimalCareConverter.toMedicalRecordResponse(saved));
    }
    @GetMapping("/{animalId}/caretakers")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<CaretakerResponse>> fetchCareTakersByAnimalId(@PathVariable Long animalId) {
        return ResponseEntity.ok(service.fetchCareTakersByAnimalId(animalId).stream()
                .map(AnimalCareConverter::toCaretakerResponse)
                .toList());
    }
    @PostMapping("/{animalId}/caretakers/{caretakerId}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> assignCareTakerToAnimal(@PathVariable Long animalId, @PathVariable Long caretakerId) {
        service.assignCareTakerToAnimal(animalId, caretakerId);
        return ResponseEntity.noContent().build();
    }

}
