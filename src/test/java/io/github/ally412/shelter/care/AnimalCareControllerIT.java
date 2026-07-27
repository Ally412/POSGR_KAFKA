package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.care.dto.MedicalRecordRequest;
import io.github.ally412.shelter.common.web.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@WithMockUser(roles = "STAFF")   // every care endpoint requires STAFF
public class AnimalCareControllerIT {
    protected static final String BASE_PATH = Constants.ANIMALS;
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AnimalRepository animalRepository;
    @Autowired
    CaretakerRepository caretakerRepository;
    @Autowired
    MedicalRecordRepository medicalRecordRepository;

    // --- medical records ---------------------------------------------------

    @Test
    public void addMedicalRecordCreatesRecord() throws Exception {
        Long animalId = persistAnimal();
        MedicalRecordRequest request =
                new MedicalRecordRequest("Vaccination", LocalDate.of(2026, 5, 1), "Dr. House");
        mockMvc.perform(post(BASE_PATH + "/{animalId}/medical-records", animalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        containsString(BASE_PATH + "/" + animalId + "/medical-records/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.animalId").value(animalId))
                .andExpect(jsonPath("$.description").value("Vaccination"))
                .andExpect(jsonPath("$.treatmentDate").value("2026-05-01"))
                .andExpect(jsonPath("$.vetName").value("Dr. House"));
    }

    @Test
    public void addMedicalRecordForMissingAnimalReturns404() throws Exception {
        MedicalRecordRequest request =
                new MedicalRecordRequest("Vaccination", LocalDate.of(2026, 5, 1), "Dr. House");
        mockMvc.perform(post(BASE_PATH + "/{animalId}/medical-records", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wrong id"));
    }

    @Test
    public void addInvalidMedicalRecordReturns400() throws Exception {
        Long animalId = persistAnimal();
        String body = """
                { "description": "", "treatmentDate": null, "vetName": "" }
                """;
        mockMvc.perform(post(BASE_PATH + "/{animalId}/medical-records", animalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.description").value("must not be blank"))
                .andExpect(jsonPath("$.errors.treatmentDate").value("must not be null"))
                .andExpect(jsonPath("$.errors.vetName").value("must not be blank"));
    }

    @Test
    public void fetchMedicalRecordsByAnimalId() throws Exception {
        Long animalId = persistAnimal();
        persistMedicalRecord(animalId, "Checkup", "Dr. House");
        mockMvc.perform(get(BASE_PATH + "/{animalId}/medical-records", animalId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].animalId").value(animalId))
                .andExpect(jsonPath("$[0].description").value("Checkup"))
                .andExpect(jsonPath("$[0].vetName").value("Dr. House"));
    }

    // --- caretakers --------------------------------------------------------

    @Test
    public void assignCaretakerToAnimalAndFetch() throws Exception {
        Long animalId = persistAnimal();
        Long caretakerId = persistCaretaker("Alice", Specialization.VET);
        mockMvc.perform(post(BASE_PATH + "/{animalId}/caretakers/{caretakerId}", animalId, caretakerId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_PATH + "/{animalId}/caretakers", animalId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(caretakerId))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].specialization").value("VET"));
    }

    @Test
    public void assignMissingCaretakerReturns404() throws Exception {
        Long animalId = persistAnimal();
        mockMvc.perform(post(BASE_PATH + "/{animalId}/caretakers/{caretakerId}", animalId, 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Caretaker not found"));
    }

    @Test
    public void assignCaretakerToMissingAnimalReturns404() throws Exception {
        Long caretakerId = persistCaretaker("Alice", Specialization.VET);
        mockMvc.perform(post(BASE_PATH + "/{animalId}/caretakers/{caretakerId}", 999L, caretakerId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wrong id"));
    }

    // --- helpers -----------------------------------------------------------

    private Long persistAnimal() {
        Animal animal = new Animal();
        animal.setName("Buddy");
        animal.setSpecies(Species.DOG);
        animal.setBreed("Mix");
        animal.setIntakeDate(LocalDate.now());
        animal.setStatus(Status.SOCIALIZING);
        return animalRepository.save(animal).getId();
    }

    private Long persistCaretaker(String name, Specialization specialization) {
        Caretaker caretaker = new Caretaker();
        caretaker.setName(name);
        caretaker.setEmail("alice@shelter.io");
        caretaker.setSpecialization(specialization);
        return caretakerRepository.save(caretaker).getId();
    }

    private Long persistMedicalRecord(Long animalId, String description, String vetName) {
        MedicalRecord record = new MedicalRecord();
        record.setAnimal(animalRepository.getReferenceById(animalId));
        record.setDescription(description);
        record.setTreatmentDate(LocalDate.now());
        record.setVetName(vetName);
        return medicalRecordRepository.save(record).getId();
    }
}
