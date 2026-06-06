-- V10 : ramène l'état trial dans la table abonnement avec statut=TRIAL.
--
-- Modèle ciblé :
--   PlanAbonnement (Pro, Premium, Essai…) — inchangé
--     └── 1..N ──► TypePlanAbonnement (Mensuel, Annuel…) — inchangé
--   Abonnement → TypePlanAbonnement (NOT NULL, comme avant)
--     • statut = TRIAL pour le bonus gratuit (signup), statut paid pour souscriptions
--   Entreprise.trial_used (booléen, conservé) — marque l'essai déjà consommé
--
-- Étapes :
--  1. Ouvre la contrainte CHECK pour autoriser TRIAL côté abonnement.statut
--  2. Seed un TypePlanAbonnement par défaut sur le plan d'essai (le DataInitializer
--     ne le créait pas encore, et la migration des trials existants en a besoin)
--  3. Migre les rangées trial existantes : entreprise.trial_plan_id +
--     trial_end_date → Abonnement (statut=TRIAL, type = première durée du plan
--     d'essai, dateDebut = dateFin - 30 jours)
--  4. Drop les colonnes trial_* sur entreprise (l'entité Java ne les porte plus)

ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS abonnement_statut_check;
ALTER TABLE abonnement ADD CONSTRAINT abonnement_statut_check
    CHECK (statut IN ('ACTIF', 'EXPIRE', 'SUSPENDU', 'EN_ATTENTE', 'TRIAL'));

INSERT INTO type_plan_abonnement (id, plan_abonnement_id, nom, duree_mois, actif, recommande, ordre, created_at, updated_at)
SELECT gen_random_uuid(), plan_abonnement.id, 'Essai', 1, true, false, 0, now(), now()
FROM plan_abonnement
WHERE plan_abonnement.trial = true
  AND plan_abonnement.actif = true
  AND NOT EXISTS (
      SELECT 1 FROM type_plan_abonnement type
      WHERE type.plan_abonnement_id = plan_abonnement.id
  );

INSERT INTO abonnement (id, entreprise_id, type_plan_abonnement_id, date_debut, date_fin, actif, renouvellement_auto, statut, created_at, updated_at)
SELECT
    gen_random_uuid(),
    entreprise.id,
    (SELECT type.id
     FROM type_plan_abonnement type
     WHERE type.plan_abonnement_id = entreprise.trial_plan_id
     ORDER BY type.ordre ASC
     LIMIT 1),
    COALESCE(entreprise.trial_end_date - interval '30 days', current_date),
    entreprise.trial_end_date,
    true,
    false,
    'TRIAL',
    now(),
    now()
FROM entreprise
WHERE entreprise.trial_plan_id IS NOT NULL
  AND entreprise.trial_end_date IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM abonnement
      WHERE abonnement.entreprise_id = entreprise.id
        AND abonnement.statut = 'TRIAL'
  );

ALTER TABLE entreprise DROP CONSTRAINT IF EXISTS fk_entreprise_trial_plan;
DROP INDEX IF EXISTS idx_entreprise_trial_plan_id;
ALTER TABLE entreprise DROP COLUMN IF EXISTS trial_plan_id;
ALTER TABLE entreprise DROP COLUMN IF EXISTS trial_end_date;
