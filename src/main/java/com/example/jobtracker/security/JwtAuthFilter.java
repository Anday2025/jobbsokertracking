package com.example.jobtracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter som autentiserer innkommende forespørsler basert på JWT-token
 * lagret i cookie.
 * <p>
 * Filteret kjøres én gang per request og forsøker å lese {@code SESSION}-cookien,
 * validere tokenet og sette autentisert bruker i Spring Security-konteksten.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Oppretter et nytt JWT-autentiseringsfilter.
     *
     * @param jwtService tjeneste for lesing og validering av JWT-token
     * @param userDetailsService tjeneste for lasting av brukerdata
     */
    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Angir om filteret skal hoppes over for en gitt forespørsel.
     * <p>
     * Filteret brukes bare for API-kall under {@code /api}, men ikke for
     * åpne autentiseringsendepunkter under {@code /api/auth}.
     *
     * @param request innkommende HTTP-forespørsel
     * @return {@code true} dersom filteret ikke skal kjøres, ellers {@code false}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if (path == null || !path.startsWith("/api")) return true;

        return path.startsWith("/api/auth");
    }

    /**
     * Utfører JWT-basert autentisering for gjeldende forespørsel.
     * <p>
     * Metoden leser token fra cookie, validerer tokenet, laster brukeren og
     * setter autentisering i security context dersom tokenet er gyldig.
     * Ved ugyldig token settes ingen autentisering, og requesten sendes videre.
     *
     * @param request innkommende HTTP-forespørsel
     * @param response utgående HTTP-respons
     * @param filterChain filterkjeden forespørselen sendes videre i
     * @throws ServletException dersom servlet-feil oppstår
     * @throws IOException dersom IO-feil oppstår
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path != null && path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = readCookie(request, "SESSION");

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception ignored) {
            // Ugyldig token -> ingen auth settes
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Leser en navngitt cookie fra forespørselen.
     *
     * @param request HTTP-forespørsel
     * @param name navn på cookien som skal leses
     * @return cookie-verdi dersom funnet, ellers {@code null}
     */
    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}