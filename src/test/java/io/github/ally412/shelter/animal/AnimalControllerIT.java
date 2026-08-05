package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.common.web.Constants;
import io.github.ally412.shelter.messaging.AnimalAddedEvent;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@WithMockUser(roles = "STAFF")   // security is on: run these as an authenticated STAFF (covers reads via hierarchy + writes)
public class AnimalControllerIT {

    // These tests don't exercise messaging. Without this, saveAnimal's send() would look for a
    // broker at the localhost:9092 default — passing only on a machine where Compose happens to
    // be up, and blocking for max.block.ms then failing on CI. Publishing is covered by
    // AnimalEventPublishingIT, which runs a real broker.
    @MockitoBean
    KafkaTemplate<String, AnimalAddedEvent> kafkaTemplate;
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

    @Test
    public void saveValidAnimal() throws Exception {
        AnimalRequest animalRequest = new AnimalRequest("Buddy", Species.DOG, "Mix", Status.SOCIALIZING);
        mockMvc.perform(post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(animalRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_PATH + "/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Buddy"))
                .andExpect(jsonPath("$.species").value("DOG"))
                .andExpect(jsonPath("$.breed").value("Mix"))
                .andExpect(jsonPath("$.status").value("SOCIALIZING"));
    }
    @Test
    public void saveInvalidAnimalBadStatus() throws Exception {
        String body = """                                                                                                                                                                     
      { "name": "Buddy", "species": "DOG", "breed": "Mix", "status": "FLYING" }                                                                                                          
      """;
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect((jsonPath("$.title").value("Invalid enum value")))
                .andExpect((jsonPath("$.detail").value("Unknown Status \"FLYING\"")));
    }
    @Test
    public void saveInvalidAnimalBlankName() throws Exception {
        AnimalRequest animalRequest = new AnimalRequest("", null, "", null);
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(animalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").value("must not be blank"))
                .andExpect(jsonPath("$.errors.species").value("must not be null"))
                .andExpect(jsonPath("$.errors.breed").value("must not be blank"))
                .andExpect(jsonPath("$.errors.status").value("must not be null"));
    }
    @Test
    public void getExistingAnimalById() throws Exception {
        Long animalId = persistAnimal();
        mockMvc.perform(get(BASE_PATH + "/{id}", animalId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(animalId))
                .andExpect(jsonPath("$.name").value("Buddy"))
                .andExpect(jsonPath("$.species").value("DOG"))
                .andExpect(jsonPath("$.breed").value("Mix"))
                .andExpect(jsonPath("$.status").value("SOCIALIZING"));

    }
    @Test
    public void getNonExistingAnimalById() throws Exception {
        Long animalId = 42L;
        mockMvc.perform(get(BASE_PATH + "/{id}", animalId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void searchBySpeciesFiltersResults() throws Exception {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, LocalDate.now());
        mockMvc.perform(get(BASE_PATH + "/search").param("species", "DOG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rex"))
                .andExpect(jsonPath("$.content[0].species").value("DOG"));
    }

    @Test
    public void searchByMultipleCriteriaCombinesWithAnd() throws Exception {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Buddy", Species.DOG, Status.SOCIALIZING, LocalDate.now());
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, LocalDate.now());
        mockMvc.perform(get(BASE_PATH + "/search")
                        .param("species", "DOG")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rex"));
    }

    @Test
    public void searchWithNoCriteriaReturnsAll() throws Exception {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, LocalDate.now());
        mockMvc.perform(get(BASE_PATH + "/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    public void searchByIntakeDateRange() throws Exception {
        persistAnimal("Old", Species.DOG, Status.AVAILABLE, LocalDate.of(2026, 1, 1));
        persistAnimal("New", Species.DOG, Status.AVAILABLE, LocalDate.of(2026, 6, 1));
        mockMvc.perform(get(BASE_PATH + "/search")
                        .param("intakeFrom", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("New"));
    }

    @Test
    public void searchRespectsPageSize() throws Exception {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Buddy", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Max", Species.DOG, Status.AVAILABLE, LocalDate.now());
        mockMvc.perform(get(BASE_PATH + "/search")
                        .param("size", "2")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    private Long persistAnimal() {
        return persistAnimal("Buddy", Species.DOG, Status.SOCIALIZING, LocalDate.now());
    }

    private Long persistAnimal(String name, Species species, Status status, LocalDate intakeDate) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setSpecies(species);
        animal.setBreed("Mix");
        animal.setIntakeDate(intakeDate);
        animal.setStatus(status);
        return animalRepository.save(animal).getId();
    }
































}
