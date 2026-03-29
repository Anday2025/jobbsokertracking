-- =========================================
-- V1__init.sql
-- Flyway initial migration for Job Tracker
-- =========================================
--
-- Formål:
-- Oppretter grunnleggende databasetabeller for applikasjonen.
--
-- Inneholder:
-- - users
-- - job_application
-- - password_reset_token
-- - verification_token
--
-- Relasjoner:
-- - En bruker kan eie mange jobbsøknader
-- - En bruker kan ha flere passordreset-tokens
-- - En bruker kan ha ett aktivt verifiseringstoken
--
-- Kommentar:
-- Denne migrasjonen er ment som første schema-oppsett og kjøres av Flyway.

-- =========================================
-- USERS
-- Lagrer brukerkontoer brukt til autentisering og autorisering.
-- =========================================
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- =========================================
-- JOB_APPLICATION
-- Lagrer jobbsøknader opprettet av brukere.
--
-- Viktige felt:
-- - company: navn på selskapet
-- - role: stillingstittel
-- - link: lenke til stillingsannonse
-- - deadline: søknadsfrist
-- - status: nåværende status for søknaden
--
-- Relasjon:
-- - user_id peker til users(id)
-- - ON DELETE CASCADE sørger for at jobbsøknader slettes
--   automatisk når brukeren slettes
-- =========================================
CREATE TABLE job_application (
                                 id BIGSERIAL PRIMARY KEY,
                                 company VARCHAR(255),
                                 role VARCHAR(255),
                                 link VARCHAR(1000),
                                 deadline DATE,
                                 status VARCHAR(50),

                                 user_id BIGINT NOT NULL,
                                 CONSTRAINT fk_job_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE
);

-- Indeks for raskere oppslag av jobbsøknader per bruker
CREATE INDEX idx_job_application_user_id
    ON job_application(user_id);

-- =========================================
-- PASSWORD_RESET_TOKEN
-- Lagrer tokens brukt ved passordtilbakestilling.
--
-- Design:
-- - token brukes som primærnøkkel
-- - flere reset-tokens kan i praksis knyttes til samme bruker
--
-- Relasjon:
-- - user_id peker til users(id)
-- - ON DELETE CASCADE sletter tokens når brukeren slettes
-- =========================================
CREATE TABLE password_reset_token (
                                      token VARCHAR(255) PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      expires_at TIMESTAMPTZ,
                                      used BOOLEAN NOT NULL DEFAULT FALSE,

                                      CONSTRAINT fk_password_reset_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users(id)
                                              ON DELETE CASCADE
);

-- Indeks for raskere oppslag av reset-tokens per bruker
CREATE INDEX idx_password_reset_token_user_id
    ON password_reset_token(user_id);

-- =========================================
-- VERIFICATION_TOKEN
-- Lagrer tokens brukt for e-postverifisering.
--
-- Design:
-- - token brukes som primærnøkkel
-- - user_id er UNIQUE, som betyr at en bruker kun kan ha
--   ett aktivt verifiseringstoken om gangen
--
-- Relasjon:
-- - user_id peker til users(id)
-- - ON DELETE CASCADE sletter token når brukeren slettes
-- =========================================
CREATE TABLE IF NOT EXISTS verification_token (
                                                  token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_verification_token_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

-- Indeks for raskere oppslag av verifiseringstoken per bruker
CREATE INDEX IF NOT EXISTS idx_verification_token_user_id
    ON verification_token(user_id);