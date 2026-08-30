CREATE TABLE facturation (
    id                  UUID NOT NULL,
    moyen_paiement_id   UUID NOT NULL,
    pays_id             UUID NULL,
    numero_facturation  VARCHAR(100) NOT NULL,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT pk_facturation PRIMARY KEY (id),
    CONSTRAINT fk_facturation_moyen_paiement FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement (id),
    CONSTRAINT fk_facturation_pays FOREIGN KEY (pays_id) REFERENCES country (id)
);

CREATE UNIQUE INDEX facturation_moyen_pays_key
    ON facturation (moyen_paiement_id, pays_id)
    WHERE pays_id IS NOT NULL;

CREATE UNIQUE INDEX facturation_moyen_global_key
    ON facturation (moyen_paiement_id)
    WHERE pays_id IS NULL;
