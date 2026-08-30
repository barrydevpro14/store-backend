CREATE TABLE moyen_paiement_pays (
    moyen_paiement_id UUID NOT NULL,
    country_id UUID NOT NULL,
    CONSTRAINT pk_moyen_paiement_pays PRIMARY KEY (moyen_paiement_id, country_id),
    CONSTRAINT fk_moyen_paiement_pays_moyen FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement (id),
    CONSTRAINT fk_moyen_paiement_pays_country FOREIGN KEY (country_id) REFERENCES country (id)
);
