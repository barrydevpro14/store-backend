-- V10 : création de la table activite_economique + rattachement à entreprise.
--
-- 1. Crée la table activite_economique (référentiel global, admin-scoped).
-- 2. Insère les 11 activités seedées (idempotent avec DataInitializer via findByLibelle).
-- 3. Ajoute la colonne activite_economique_id sur entreprise en nullable.
-- 4. Backfille les entreprises existantes avec "pièces détachées".
-- 5. Pose le NOT NULL + contrainte FK.

CREATE TABLE activite_economique (
    id          UUID         PRIMARY KEY,
    libelle     VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

INSERT INTO activite_economique (id, libelle) VALUES
    (gen_random_uuid(), 'ALIMENTATION'),
    (gen_random_uuid(), 'BOULANGERIE'),
    (gen_random_uuid(), 'COIFFURE'),
    (gen_random_uuid(), 'ELECTRONIQUE'),
    (gen_random_uuid(), 'LIBRAIRIE'),
    (gen_random_uuid(), 'PAPETERIE'),
    (gen_random_uuid(), 'PHARMACIE'),
    (gen_random_uuid(), 'PIÈCES DÉTACHÉES'),
    (gen_random_uuid(), 'QUINCAILLERIE'),
    (gen_random_uuid(), 'RESTAURANT'),
    (gen_random_uuid(), 'VETEMENT');

ALTER TABLE entreprise ADD COLUMN activite_economique_id UUID;

UPDATE entreprise
SET activite_economique_id = (
    SELECT id FROM activite_economique WHERE libelle = 'PIÈCES DÉTACHÉES'
);

ALTER TABLE entreprise
    ALTER COLUMN activite_economique_id SET NOT NULL,
    ADD CONSTRAINT fk_entreprise_activite_economique
        FOREIGN KEY (activite_economique_id) REFERENCES activite_economique (id);
