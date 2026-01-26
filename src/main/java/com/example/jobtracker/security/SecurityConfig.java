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

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ✅ API + cookie auth → vi bruker ikke CSRF her
                .csrf(csrf -> csrf.disable())

                // ✅ CORS config under
                .cors(Customizer.withDefaults())

                // ✅ JWT/cookie = stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ✅ Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Frontend + assets
                        .requestMatchers(
                                "/", "/index.html",
                                "/styles.css", "/app.js",
                                "/favicon.ico",
                                "/**/*.css", "/**/*.js", "/**/*.map",
                                "/**/*.png", "/**/*.jpg", "/**/*.jpeg",
                                "/**/*.svg", "/**/*.webp", "/**/*.ico"
                        ).permitAll()

                        // ✅ Spring error endpoint (viktig for å slippe rare 401/redirect loops)
                        .requestMatchers("/error").permitAll()

                        // ✅ ALLE auth-endepunkter skal alltid være åpne
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ alt annet: må være innlogget
                        .anyRequest().authenticated()
                )

                // ✅ Når noen ikke er innlogget
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("text/plain; charset=utf-8");
                    res.getWriter().write("Unauthorized");
                }))

                // ✅ JWT-filter før username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ Bruk patterns (bedre enn allowedOrigins når credentials=true)
        cfg.setAllowedOriginPatterns(List.of(
                "https://*.onrender.com",
                "http://localhost:*"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        // (valgfritt) hvis du vil lese Set-Cookie i browser devtools
        cfg.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
