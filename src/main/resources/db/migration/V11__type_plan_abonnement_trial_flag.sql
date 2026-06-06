-- V11 : déplace le marqueur d'essai gratuit du Plan vers le Type.
--
-- Modèle : `TypePlanAbonnement.trial = true` désigne désormais le type
-- utilisé par le flux signup pour créer l'Abonnement avec statut TRIAL.
-- {@code PlanAbonnement.trial} reste utilisé pour le catalogue public
-- (filtrage / badge) et le refus de subscribe sur un plan d'essai.
--
-- Backfill : les types seedés par V10 (Essai) sont marqués trial=true.
-- En pratique il n'en existe qu'un par installation, mais on cible via
-- la chaîne nom='Essai' + plan.trial=true pour rester déterministe.

ALTER TABLE type_plan_abonnement
    ADD COLUMN trial boolean NOT NULL DEFAULT false;

UPDATE type_plan_abonnement
SET trial = true
WHERE id IN (
    SELECT type.id
    FROM type_plan_abonnement type
    JOIN plan_abonnement plan ON plan.id = type.plan_abonnement_id
    WHERE plan.trial = true
);
