-- V9 : re-attribue une fenêtre d'essai à toute entreprise en limbo.
--
-- Contexte : V8 ne backfille que les entreprises avec `trial_used = false`.
-- Certaines entreprises héritées de la base avant V7 portaient déjà
-- `trial_used = true` (consommation passée), mais n'ont plus aucun
-- Abonnement payé (purgé par V7) — elles se retrouvent bloquées au
-- login par le gate `auth.subscription.required`.
--
-- Stratégie one-off : pour toute entreprise sans Abonnement et sans
-- fenêtre d'essai vivante (trial_end_date IS NULL OU < today), réattribue
-- le plan d'essai actif et remet `trial_used = false`. Cette migration ne
-- doit plus jamais déclencher en prod — c'est une remédiation post-V7
-- spécifique au dev.

UPDATE entreprise SET
    trial_plan_id = (SELECT id
                     FROM plan_abonnement
                     WHERE trial = true AND actif = true
                     ORDER BY ordre ASC
                     LIMIT 1),
    trial_end_date = current_date + interval '30 days',
    trial_used = false
WHERE NOT EXISTS (
        SELECT 1 FROM abonnement WHERE abonnement.entreprise_id = entreprise.id
    )
  AND (trial_end_date IS NULL OR trial_end_date < current_date)
  AND EXISTS (
        SELECT 1 FROM plan_abonnement WHERE trial = true AND actif = true
    );
