package com.example.jobtracker.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enkel in-memory rate limiter.
 * <p>
 * Klassen begrenser hvor mange forespørsler en gitt nøkkel kan gjøre
 * innenfor et bestemt tidsvindu.
 */
@Service
public class RateLimitService {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /**
     * Sjekker om en forespørsel skal tillates innenfor gitt rate limit.
     * <p>
     * Metoden lagrer timestamps per nøkkel og fjerner gamle treff utenfor
     * tidsvinduet før ny avgjørelse tas.
     *
     * @param key identifikator for klient, bruker eller IP
     * @param maxHits maksimalt antall tillatte treff i tidsvinduet
     * @param windowMs lengde på tidsvindu i millisekunder
     * @return {@code true} dersom forespørselen tillates, ellers {@code false}
     */
    public boolean allow(String key, int maxHits, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (q) {
            while (!q.isEmpty() && (now - q.peekFirst()) > windowMs) q.pollFirst();
            if (q.size() >= maxHits) return false;
            q.addLast(now);
            return true;
        }
    }
}