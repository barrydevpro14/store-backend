-- V8 : backfill l'état trial sur les entreprises laissées en limbo par V7.
--
-- V7 a purgé toutes les rangées d'abonnement (drop & reseed côté Abonnement)
-- et a ajouté `entreprise.trial_plan_id` + `entreprise.trial_end_date` en NULL.
-- Conséquence : les entreprises créées avant V7 se retrouvent sans Abonnement
-- ni fenêtre d'essai — un OWNER ne peut donc plus accéder à son tenant.
--
-- Cette migration attache rétroactivement le plan d'essai actif (seedé par
-- DataInitializer) à toute entreprise sans Abonnement, sans plan d'essai déjà
-- attaché et qui n'a pas encore consommé son essai. Fenêtre d'essai = 30 jours
-- (valeur par défaut de `subscription.trial-days`).

UPDATE entreprise SET
    trial_plan_id = (SELECT id
                     FROM plan_abonnement
                     WHERE trial = true AND actif = true
                     ORDER BY ordre ASC
                     LIMIT 1),
    trial_end_date = current_date + interval '30 days'
WHERE trial_used = false
  AND trial_plan_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM abonnement WHERE abonnement.entreprise_id = entreprise.id
  )
  AND EXISTS (
      SELECT 1 FROM plan_abonnement WHERE trial = true AND actif = true
  );
