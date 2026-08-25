CREATE TABLE revenu (
    id             UUID            PRIMARY KEY,
    entreprise_id  UUID            NOT NULL,
    country_id     UUID            NOT NULL,
    montant        DECIMAL(19, 2)  NOT NULL,
    date_paiement  DATE            NOT NULL,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT fk_revenu_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise(id),
    CONSTRAINT fk_revenu_country    FOREIGN KEY (country_id)    REFERENCES country(id)
);

INSERT INTO revenu (id, entreprise_id, country_id, montant, date_paiement, created_at, updated_at)
SELECT gen_random_uuid(), a.entreprise_id, e.country_id, pa.montant_final, pa.date_paiement, pa.created_at, pa.updated_at
FROM paiement_abonnement pa
JOIN abonnement a  ON pa.abonnement_id = a.id
JOIN entreprise e  ON a.entreprise_id  = e.id
WHERE pa.statut = 'VALIDE'
  AND pa.date_paiement IS NOT NULL
  AND pa.montant_final IS NOT NULL;
