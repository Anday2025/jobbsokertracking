package com.example.jobtracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * Konfigurasjonsklasse for sikkerhetsrelaterte Spring-beans.
 * <p>
 * Klassen eksponerer {@link AuthenticationManager} som en Spring-bean,
 * slik at den kan injiseres og brukes av andre deler av applikasjonen ved behov.
 */
@Configuration
public class AuthBeans {

    /**
     * Oppretter en {@link AuthenticationManager}-bean basert på Spring Securitys
     * standard {@link AuthenticationConfiguration}.
     *
     * @param config Spring Security-konfigurasjon for autentisering
     * @return konfigurert authentication manager
     * @throws Exception dersom authentication manager ikke kan opprettes
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}