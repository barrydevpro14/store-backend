-- Rapport d'inventaire : consolidation comptable produite lors de la cloture d'un
-- inventaire physique. Compare le stock theorique vs physique (valorise prixAchat),
-- la caisse, les depenses et le fond de roulement de la periode pour calculer le
-- benefice net (montantPhysique + montantCaisse - depense - montantRoulement).
-- Relation 1-1 avec inventaire : un seul rapport peut etre produit par inventaire.

CREATE TABLE rapport_inventaire (
    id                   UUID PRIMARY KEY,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255),
    inventaire_id        UUID           NOT NULL UNIQUE,
    montant_automatique  NUMERIC(19, 2) NOT NULL,
    montant_physique     NUMERIC(19, 2) NOT NULL,
    ecart                NUMERIC(19, 2) NOT NULL,
    montant_caisse       NUMERIC(19, 2) NOT NULL,
    depense              NUMERIC(19, 2) NOT NULL,
    montant_roulement    NUMERIC(19, 2) NOT NULL,
    date_debut_periode   DATE           NOT NULL,
    date_fin_periode     DATE           NOT NULL,
    benefice             NUMERIC(19, 2) NOT NULL,
    status               VARCHAR(20)    NOT NULL
);

ALTER TABLE rapport_inventaire
    ADD CONSTRAINT fk_rapport_inventaire
        FOREIGN KEY (inventaire_id) REFERENCES inventaire (id) ON DELETE CASCADE;

CREATE INDEX idx_rapport_inventaire_status ON rapport_inventaire (status);
