-- V65: Subscription refonte — supprimer TypePlanAbonnement, simplifier Abonnement
--
-- plan_abonnement : ajoute trial (boolean, backfill depuis TypePlanAbonnement.trial)
-- abonnement      : supprime type_plan_abonnement_id, actif, renouvellement_auto
--                   ajoute prochain_plan_abonnement_id (nullable FK → plan_abonnement)
-- type_plan_abonnement : table supprimée

-- 1. Ajoute le flag trial sur plan_abonnement (backfill depuis le type marqué trial)
ALTER TABLE plan_abonnement ADD COLUMN trial boolean NOT NULL DEFAULT false;

UPDATE plan_abonnement p
SET trial = true
WHERE EXISTS (
    SELECT 1 FROM type_plan_abonnement t
    WHERE t.plan_abonnement_id = p.id
      AND t.trial = true
);

-- 2. Supprime l'index partiel qui dépend de la colonne actif
DROP INDEX IF EXISTS abonnement_one_actif_per_entreprise;

-- 3. Supprime la FK + l'index sur type_plan_abonnement_id dans abonnement
ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS fk_abonnement_type_plan_abonnement;
DROP INDEX IF EXISTS idx_abonnement_type_plan_abonnement_id;
ALTER TABLE abonnement DROP COLUMN IF EXISTS type_plan_abonnement_id;

-- 4. Supprime actif et renouvellement_auto (AbonnementStatut seul pilote l'état)
ALTER TABLE abonnement DROP COLUMN IF EXISTS actif;
ALTER TABLE abonnement DROP COLUMN IF EXISTS renouvellement_auto;

-- 5. Ajoute prochain_plan_abonnement_id (null = pas de changement de plan demandé)
ALTER TABLE abonnement ADD COLUMN prochain_plan_abonnement_id uuid;
ALTER TABLE abonnement
    ADD CONSTRAINT fk_abonnement_prochain_plan
        FOREIGN KEY (prochain_plan_abonnement_id) REFERENCES plan_abonnement(id);

-- 6. Supprime la table type_plan_abonnement (plus aucune référence après step 3)
DROP TABLE IF EXISTS type_plan_abonnement;
