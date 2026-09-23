package io.github.mabdulloh.booklending.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI bookLendingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book Lending API")
                        .description("""
                                REST microservice for managing a book catalog, library members, and loans.

                                ## Authentication

                                All endpoints (except `/api/v1/auth/login`) require a Bearer JWT in the
                                `Authorization` header. Obtain a token via the login endpoint. Click
                                **Authorize** (top right) and paste the token.

                                ## Roles

                                - `ADMIN` — full access (manage books and members)
                                - `MEMBER` — borrow and return books
                                """)
                        .version("v0.0.1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtained from POST /api/v1/auth/login")));
    }
}