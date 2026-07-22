package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalRequest;
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
    protected static final String BASE_PATH = Constants.API + "/animals";
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

    private Long persistAnimal() {
        Animal animal = new Animal();
        animal.setName("Buddy");
        animal.setSpecies(Species.DOG);
        animal.setBreed("Mix");
        animal.setIntakeDate(LocalDate.now());
        animal.setStatus(Status.SOCIALIZING);
        return animalRepository.save(animal).getId();
    }
































}
