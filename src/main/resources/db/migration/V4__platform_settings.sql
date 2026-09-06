-- V4: Platform settings - simple key/value store for admin-configurable platform settings
-- ======================================================================================

CREATE TABLE platform_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(150) NOT NULL UNIQUE,
    setting_value VARCHAR(500),
    description VARCHAR(500),
    editable BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed a small set of genuinely relevant settings.
-- cors.allowed-origins and jwt.expiration-minutes are informational-only copies of
-- static application config (app.cors.allowed-origins / app.jwt.expiration-ms) - they
-- are NOT re-read dynamically, hence editable = FALSE.
INSERT INTO platform_setting (setting_key, setting_value, description, editable) VALUES
    ('cors.allowed-origins', 'http://localhost:4200',
     'Informational copy of the app.cors.allowed-origins configuration. Read-only: changing this value has no runtime effect.',
     FALSE),
    ('jwt.expiration-minutes', '60',
     'Informational copy of the app.jwt.expiration-ms configuration (in minutes). Read-only: changing this value has no runtime effect.',
     FALSE),
    ('policies.default-tenure-months-override', '0',
     'When set to a positive number, overrides the product tenure (in months) used to calculate new policy end dates and renewal end dates. Set to 0 to disable the override and use each product''s own tenure.',
     TRUE);
