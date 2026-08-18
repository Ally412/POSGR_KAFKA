package io.github.ally412.shelter.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Generated documentation can rot quietly: springdoc only describes the endpoints it can see, so
 * a controller behind a misconfigured filter, or a security rule that hides the document itself,
 * produces a plausible-looking but incomplete spec. This asserts the document is reachable
 * without a token and actually lists the endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OpenApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // The relay ticks in every @SpringBootTest; mocked so it never reaches for a broker.
    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /** No token: a client has to read the description before it knows how to authenticate. */
    @Test
    void apiDocsAreReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void everyControllerIsDocumented() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode paths = objectMapper.readTree(body).get("paths");

        assertThat(paths.propertyNames()).contains(
                "/api/animals",
                "/api/animals/{id}",
                "/api/animals/search",
                "/api/animals/{animalId}/medical-records",
                "/api/animals/{animalId}/caretakers",
                "/api/animals/{animalId}/adoption",
                "/auth/login",
                "/auth/register",
                "/accounts");
    }

    /** Without this, Swagger UI renders the API but cannot call any of it. */
    @Test
    void bearerAuthenticationIsDeclared() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode scheme = objectMapper.readTree(body)
                .get("components").get("securitySchemes").get("bearer-jwt");

        assertThat(scheme).isNotNull();
        assertThat(scheme.get("scheme").asString()).isEqualTo("bearer");
        assertThat(scheme.get("bearerFormat").asString()).isEqualTo("JWT");
    }
}
