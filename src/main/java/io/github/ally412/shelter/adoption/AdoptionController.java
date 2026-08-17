package io.github.ally412.shelter.adoption;

import io.github.ally412.shelter.adoption.dto.AdoptionConverter;
import io.github.ally412.shelter.adoption.dto.AdoptionRequest;
import io.github.ally412.shelter.adoption.dto.AdoptionResponse;
import io.github.ally412.shelter.common.web.Constants;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(Constants.ANIMALS)
public class AdoptionController {

    private final AdoptionService adoptionService;

    public AdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @PostMapping("/{animalId}/adoption")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<AdoptionResponse> completeAdoption(
            @PathVariable Long animalId,
            @Valid @RequestBody AdoptionRequest adoptionRequest) {
        Adoption adoption = adoptionService.completeAdoption(
                animalId, adoptionRequest.adopterId(), adoptionRequest.date());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(Constants.ANIMALS + "/{animalId}/adoption")
                .buildAndExpand(animalId)
                .toUri();
        return ResponseEntity.created(location).body(AdoptionConverter.toAdoptionResponse(adoption));
    }
}
