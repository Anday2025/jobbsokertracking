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
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ✅ preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Viktig: /error må være åpen ellers får du 401 i frontend når noe feiler
                        .requestMatchers("/error").permitAll()

                        // ✅ frontend (statiske filer)
                        .requestMatchers(
                                "/", "/index.html",
                                "/styles.css", "/app.js",
                                "/favicon.ico",
                                "/**/*.css", "/**/*.js",
                                "/**/*.png", "/**/*.jpg", "/**/*.jpeg",
                                "/**/*.svg", "/**/*.webp", "/**/*.ico", "/**/*.map"
                        ).permitAll()

                        // ✅ auth endepunkter (register/login/verify/forgot/reset/resend)
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ alt annet må være innlogget
                        .anyRequest().authenticated()
                )

                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("text/plain; charset=utf-8");
                    res.getWriter().write("Unauthorized");
                }))

                // ✅ JWT filter før standard Spring Security filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ Må være eksakt origin match når credentials=true
        cfg.setAllowedOrigins(List.of(
                "https://job-tracker-0qv9.onrender.com",
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://localhost:63342"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ viktig: content-type må være med + authorization
        cfg.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization"));

        // ✅ cookies funker bare når dette er true
        cfg.setAllowCredentials(true);

        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
