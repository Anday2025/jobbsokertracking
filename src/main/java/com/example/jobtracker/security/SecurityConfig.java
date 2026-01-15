package com.example.jobtracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // enklest nå (kan aktiveres senere)
                .cors(cors -> {})             // ok selv om du ikke har egen CORS bean

                // ✅ Sessions på (ikke STATELESS)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ✅ IKKE basic auth og IKKE formLogin (da slipper du popup og redirect)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())

                .authorizeHttpRequests(auth -> auth
                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // statiske filer + error
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/styles.css",
                                "/app.js",
                                "/favicon.ico",
                                "/error",
                                "/**/*.css",
                                "/**/*.js",
                                "/**/*.map",
                                "/**/*.png",
                                "/**/*.jpg",
                                "/**/*.jpeg",
                                "/**/*.svg",
                                "/**/*.webp",
                                "/**/*.ico"
                        ).permitAll()

                        // auth endpoints åpne
                        .requestMatchers("/api/auth/**").permitAll()

                        // alt annet krever login (session)
                        .anyRequest().authenticated()
                )

                // ✅ 401 uten popup
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("text/plain; charset=utf-8");
                    res.getWriter().write("Unauthorized");
                }))

                // ✅ logout endpoint (optional, vi lager også controller endpoint)
                .logout(l -> l.logoutUrl("/api/auth/logout"));

        return http.build();
    }
}
