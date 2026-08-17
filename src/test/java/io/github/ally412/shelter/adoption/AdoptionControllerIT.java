package io.github.ally412.shelter.adoption;

import io.github.ally412.shelter.adoption.dto.AdoptionRequest;
import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.AnimalRepository;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.StatusRequest;
import io.github.ally412.shelter.common.web.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@WithMockUser(roles = "STAFF")
class AdoptionControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // OutboxRelay ticks during this test too; mocked so it never reaches a broker.
    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    AdopterRepository adopterRepository;

    private Animal animal;
    private Adopter adopter;

    @BeforeEach
    void setUp() {
        Animal a = new Animal();
        a.setName("Rex");
        a.setSpecies(Species.DOG);
        a.setBreed("Husky");
        a.setStatus(Status.AVAILABLE);
        a.setIntakeDate(LocalDate.of(2026, 1, 15));
        animal = animalRepository.save(a);

        Adopter ad = new Adopter();
        ad.setName("Maria");
        ad.setEmail("maria@example.com");
        adopter = adopterRepository.save(ad);
    }

    private String adoptionPath(Long animalId) {
        return Constants.ANIMALS + "/" + animalId + "/adoption";
    }

    private String body(Long adopterId, LocalDate date) {
        return objectMapper.writeValueAsString(new AdoptionRequest(adopterId, date));
    }

    @Test
    void completeAdoptionReturnsCreated() throws Exception {
        mockMvc.perform(post(adoptionPath(animal.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(adopter.getId(), LocalDate.of(2026, 8, 13))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/adoption")))
                .andExpect(jsonPath("$.animalId").value(animal.getId()))
                .andExpect(jsonPath("$.animalName").value("Rex"))
                .andExpect(jsonPath("$.adopterId").value(adopter.getId()))
                .andExpect(jsonPath("$.adopterName").value("Maria"))
                .andExpect(jsonPath("$.date").value("2026-08-13"));
    }

    @Test
    void secondAdoptionOfTheSameAnimalIsConflict() throws Exception {
        mockMvc.perform(post(adoptionPath(animal.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(adopter.getId(), LocalDate.of(2026, 8, 13))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(adoptionPath(animal.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(adopter.getId(), LocalDate.of(2026, 8, 14))))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownAdopterIsNotFound() throws Exception {
        mockMvc.perform(post(adoptionPath(animal.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(-1L, LocalDate.of(2026, 8, 13))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownAnimalIsNotFound() throws Exception {
        mockMvc.perform(post(adoptionPath(-1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(adopter.getId(), LocalDate.of(2026, 8, 13))))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureAdoptionDateIsRejected() throws Exception {
        mockMvc.perform(post(adoptionPath(animal.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(adopter.getId(), LocalDate.now().plusYears(1))))
                .andExpect(status().isBadRequest());
    }

    /**
     * ADOPTED must not be reachable through the generic status endpoint — that path writes no
     * adoption row and publishes nothing, so it would leave the two systems disagreeing.
     */
    @Test
    void statusEndpointRefusesToSetAdopted() throws Exception {
        mockMvc.perform(patch(Constants.ANIMALS + "/" + animal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusRequest(Status.ADOPTED))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(Constants.ANIMALS + "/" + animal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusRequest(Status.SOCIALIZING))))
                .andExpect(status().isOk());
    }
}
