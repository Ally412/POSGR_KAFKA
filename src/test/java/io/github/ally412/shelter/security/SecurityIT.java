package io.github.ally412.shelter.security;

import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import io.github.ally412.shelter.animal.dto.AnimalRequest;
import io.github.ally412.shelter.security.dto.LoginRequest;
import io.github.ally412.shelter.security.dto.TokenResponse;
import io.github.ally412.shelter.users.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class SecurityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    // ---------- helpers ----------

    private void register(String username, String password, String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, password, email))))
                .andExpect(status().isCreated());
    }

    private String token(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class).token();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String animalJson() {
        return objectMapper.writeValueAsString(new AnimalRequest("Rex", Species.DOG, "Mix", Status.SOCIALIZING));
    }

    // ---------- tests ----------

    @Test
    void register_thenLogin_returnsToken() throws Exception {
        register("alice", "pw123456", "alice@shelter.io");
        assertFalse(token("alice", "pw123456").isBlank());
    }

    @Test
    void protectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badPassword_isUnauthorized() throws Exception {
        register("bob", "pw123456", "bob@shelter.io");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bob", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_canRead_cannotWrite() throws Exception {
        register("carol", "pw123456", "carol@shelter.io");
        String user = token("carol", "pw123456");

        mockMvc.perform(get("/api/animals").header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/animals").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(animalJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_cannotCreateStaff_isForbidden() throws Exception {
        register("erin", "pw123456", "erin@shelter.io");
        String user = token("erin", "pw123456");

        mockMvc.perform(post("/accounts").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("frank", "pw123456", "frank@shelter.io"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_createsStaff_andStaffCanWrite() throws Exception {
        String admin = token("admin", "admin");   // seeded by AdminSeeder on startup

        mockMvc.perform(post("/accounts").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("dave", "pw123456", "dave@shelter.io"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));

        String staff = token("dave", "pw123456");
        mockMvc.perform(post("/api/animals").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON).content(animalJson()))
                .andExpect(status().isCreated());
    }
}
