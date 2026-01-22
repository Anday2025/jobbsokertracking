-- =========================
-- V1__init.sql (Flyway)
-- =========================

-- USERS
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- JOB_APPLICATION
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

CREATE INDEX idx_job_application_user_id
    ON job_application(user_id);

-- PASSWORD_RESET_TOKEN (token = PK)
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

CREATE INDEX idx_password_reset_token_user_id
    ON password_reset_token(user_id);
