CREATE TABLE entreprise_setting (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    entreprise_id    UUID         NOT NULL,
    couleur_primaire VARCHAR(7),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    CONSTRAINT pk_entreprise_setting PRIMARY KEY (id),
    CONSTRAINT uk_entreprise_setting_entreprise UNIQUE (entreprise_id),
    CONSTRAINT fk_entreprise_setting_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise(id)
);
