-- V5 : restructure module Abonnement
--
-- Modèle cible :
--   PlanAbonnement (Pro, Premium, Starter, …)
--     └── 1..N ──► TypePlanAbonnement (Mensuel, Trimestriel, Annuel, …)
--   Abonnement → PlanAbonnement (NOT NULL, plan toujours connu)
--   Abonnement → TypePlanAbonnement (NULLABLE — null pour un trial,
--                                    non-null pour un abonnement payé ;
--                                    contrainte domaine : type.plan == abonnement.plan)
--
-- Stratégie : "drop & reseed" (dev only, prod sans données). Les rangées
-- existantes de type_abonnement et leurs références sur abonnement sont
-- purgées : on ne peut pas inférer à quel plan chaque type catalogue-wide
-- aurait dû appartenir.

-- 1. Purger les références côté abonnement (et ses descendants pour respecter les FK)
DELETE FROM utilisation_coupon;
DELETE FROM paiement_abonnement;
DELETE FROM abonnement;

-- 2. Renommer la table + ses contraintes
ALTER TABLE type_abonnement RENAME TO type_plan_abonnement;
ALTER TABLE type_plan_abonnement RENAME CONSTRAINT type_abonnement_pkey TO type_plan_abonnement_pkey;
ALTER TABLE type_plan_abonnement RENAME CONSTRAINT type_abonnement_reduction_type_check TO type_plan_abonnement_reduction_type_check;

-- 3. Drop l'unicité globale sur nom — l'unicité devient scopée par plan
ALTER TABLE type_plan_abonnement DROP CONSTRAINT type_abonnement_nom_key;

-- 4. Purger les rangées existantes (drop & reseed)
TRUNCATE TABLE type_plan_abonnement CASCADE;

-- 5. Ajouter la FK vers plan_abonnement (NOT NULL après purge)
ALTER TABLE type_plan_abonnement
    ADD COLUMN plan_abonnement_id uuid NOT NULL;

ALTER TABLE type_plan_abonnement
    ADD CONSTRAINT fk_type_plan_abonnement_plan
        FOREIGN KEY (plan_abonnement_id) REFERENCES plan_abonnement(id);

CREATE INDEX idx_type_plan_abonnement_plan_id ON type_plan_abonnement(plan_abonnement_id);

-- 6. Unicité scopée : un même nom (Mensuel, Annuel) peut exister sur plusieurs plans
ALTER TABLE type_plan_abonnement
    ADD CONSTRAINT uk_type_plan_abonnement_plan_nom UNIQUE (plan_abonnement_id, nom);

-- 7. Côté abonnement : renommer plan_id → plan_abonnement_id (clarté), passer NOT NULL
ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS fk18sq3qv8maxr6sri8ngui8wc4;
ALTER TABLE abonnement RENAME COLUMN plan_id TO plan_abonnement_id;
ALTER TABLE abonnement ALTER COLUMN plan_abonnement_id SET NOT NULL;
ALTER TABLE abonnement
    ADD CONSTRAINT fk_abonnement_plan_abonnement
        FOREIGN KEY (plan_abonnement_id) REFERENCES plan_abonnement(id);

CREATE INDEX idx_abonnement_plan_abonnement_id ON abonnement(plan_abonnement_id);

-- 8. Renommer la FK type ; reste nullable (trial sans type)
ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS fkfjc6qrp3tpg10uhb99q786dyp;
ALTER TABLE abonnement RENAME COLUMN type_abonnement_id TO type_plan_abonnement_id;
ALTER TABLE abonnement
    ADD CONSTRAINT fk_abonnement_type_plan_abonnement
        FOREIGN KEY (type_plan_abonnement_id) REFERENCES type_plan_abonnement(id);

CREATE INDEX idx_abonnement_type_plan_abonnement_id ON abonnement(type_plan_abonnement_id);
