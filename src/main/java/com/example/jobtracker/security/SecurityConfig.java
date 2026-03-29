package com.example.jobtracker.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Hovedkonfigurasjon for Spring Security i applikasjonen.
 * <p>
 * Klassen definerer passord-encoder, filterkjede, tilgangsregler
 * og CORS-oppsett for backend-API-et.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Oppretter en ny sikkerhetskonfigurasjon.
     *
     * @param jwtAuthFilter filter for JWT-basert autentisering
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Oppretter en {@link PasswordEncoder} for hashing av passord.
     *
     * @return bcrypt-basert password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Konfigurerer applikasjonens security filter chain.
     * <p>
     * Regler:
     * <ul>
     *   <li>CSRF er deaktivert</li>
     *   <li>CORS er aktivert</li>
     *   <li>Session policy er stateless</li>
     *   <li>{@code /api/auth/**} er åpent</li>
     *   <li>Swagger-endepunkter er åpne</li>
     *   <li>andre {@code /api/**}-endepunkter krever autentisering</li>
     * </ul>
     *
     * @param http Spring Securitys HTTP-konfigurasjon
     * @return konfigurert filter chain
     * @throws Exception dersom konfigurasjonen feiler
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("text/plain; charset=utf-8");
                    res.getWriter().write("Unauthorized");
                }))

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Konfigurerer CORS-regler for frontend-klienter som kan kalle API-et.
     *
     * @return CORS-konfigurasjonskilde
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(
                "https://www.jobbsokertracking.no",
                "https://jobbsokertracking.no"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}