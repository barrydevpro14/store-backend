CREATE TABLE category_depense_plateforme (
    id          UUID            PRIMARY KEY,
    nom         VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    actif       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT uk_category_depense_plateforme_nom UNIQUE (nom)
);

CREATE TABLE depense_plateforme (
    id                 UUID            PRIMARY KEY,
    category_id        UUID            NOT NULL,
    country_id         UUID,
    moyen_paiement_id  UUID            NOT NULL,
    libelle            VARCHAR(200)    NOT NULL,
    description        TEXT,
    date_depense        DATE            NOT NULL,
    montant            DECIMAL(19, 2)  NOT NULL,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    CONSTRAINT fk_depense_plateforme_category  FOREIGN KEY (category_id) REFERENCES category_depense_plateforme(id),
    CONSTRAINT fk_depense_plateforme_country   FOREIGN KEY (country_id)  REFERENCES country(id),
    CONSTRAINT fk_depense_plateforme_moyen     FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement(id)
);
