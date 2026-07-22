package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalConverter;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.animal.dto.AnimalResponse;
import io.github.ally412.shelter.animal.dto.BulkUpdateResponse;
import io.github.ally412.shelter.animal.dto.StatusRequest;
import io.github.ally412.shelter.common.web.Constants;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AnimalController.BASE_PATH)
public class AnimalController {
    protected static final String BASE_PATH = Constants.API + "/animals";
    private final AnimalService animalService;

    public AnimalController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AnimalResponse> getAnimal(@PathVariable Long id) {
        return ResponseEntity.of(animalService.getAnimal(id).map(AnimalConverter::toAnimalResponse));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AnimalResponse>> getAnimals() {
        return ResponseEntity.ok(animalService.getAnimals().stream()
                                                            .map(AnimalConverter::toAnimalResponse)
                                                            .toList()
                                );
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<AnimalResponse> saveAnimal(@Valid @RequestBody AnimalRequest animalRequest) {
        Animal saved = animalService.saveAnimal(animalRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity
                .created(location)
                .body(AnimalConverter.toAnimalResponse(saved));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<AnimalResponse> updateAnimal(@PathVariable Long id,
                                                       @Valid @RequestBody AnimalRequest animalRequest) {
        return ResponseEntity.of(animalService.updateAnimal(id, animalRequest).map(AnimalConverter::toAnimalResponse));
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<AnimalResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody StatusRequest statusRequest) {
        return ResponseEntity.of(animalService.updateStatus(id, statusRequest.status())
                .map(AnimalConverter::toAnimalResponse));
    }
    @PatchMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<BulkUpdateResponse> updateStatusSocializingToAvailable() {
        return ResponseEntity.ok(new BulkUpdateResponse(animalService.updateStatusSocializingToAvailable()));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> deleteAnimal(@PathVariable Long id) {
        return switch (animalService.deleteAnimal(id)) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }


}
