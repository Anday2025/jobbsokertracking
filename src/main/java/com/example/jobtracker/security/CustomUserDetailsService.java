package com.example.jobtracker.security;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * Implementasjon av {@link UserDetailsService} for lasting av brukere
 * fra applikasjonens database.
 * <p>
 * Klassen brukes av Spring Security for å hente brukerdata under
 * autentisering og tokenvalidering.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    /**
     * Oppretter en ny {@code CustomUserDetailsService}.
     *
     * @param repo repository for oppslag av brukere
     */
    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    /**
     * Laster en bruker basert på e-postadresse.
     * <p>
     * Metoden brukes av Spring Security for å bygge et
     * {@link UserDetails}-objekt fra en lagret bruker i databasen.
     *
     * @param email e-postadresse brukt som brukernavn
     * @return et {@link UserDetails}-objekt for den aktuelle brukeren
     * @throws UsernameNotFoundException dersom brukeren ikke finnes
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .roles("USER")
                .build();
    }
}