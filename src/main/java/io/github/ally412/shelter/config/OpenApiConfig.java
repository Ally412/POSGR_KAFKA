package io.github.ally412.shelter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearer-jwt";

    /**
     * springdoc discovers the endpoints on its own; this only supplies what it cannot infer.
     * <p>
     * The security scheme is the part that earns its keep: every endpoint outside /auth needs a
     * bearer token, so without it Swagger UI can render the API but not call anything. Declaring
     * it adds the Authorize button — paste a token from POST /auth/login and the "Try it out"
     * buttons start working.
     */
    @Bean
    public OpenAPI shelterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Animal Shelter API")
                        .version("v1")
                        .description("""
                                Animals, medical records and adoptions, with domain events \
                                published to Kafka through a transactional outbox. \
                                See KafkaPipeline.md for how an event travels to the notifier."""))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
