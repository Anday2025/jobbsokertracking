package com.example.jobtracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasjon for OpenAPI (Swagger) dokumentasjon.
 * <p>
 * Denne klassen definerer:
 * <ul>
 *     <li>API-informasjon (tittel, beskrivelse, versjon)</li>
 *     <li>Kontaktinformasjon</li>
 *     <li>Sikkerhetsoppsett (JWT Bearer token)</li>
 * </ul>
 * <p>
 * Brukes av springdoc-openapi for å generere Swagger UI automatisk.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Oppretter og konfigurerer OpenAPI-dokumentasjonen for applikasjonen.
     *
     * @return konfigurert {@link OpenAPI} objekt med metadata og sikkerhetsoppsett
     */
    @Bean
    public OpenAPI jobTrackerOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Jobbsøker Tracker API")
                        .description("REST API for tracking job applications, authentication, email verification, and password reset.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Anday Semere")
                                .email("andaysemere568@yahoo.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}