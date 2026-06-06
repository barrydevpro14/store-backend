-- V13: Drop the orphan `plan_abonnement.trial` column.
--
-- The `trial` field was removed from the PlanAbonnement entity in commit
-- 6db81b2 ("remove trial on plan abonnement"), but no migration ever
-- dropped the matching DB column. The column stayed defined in V1 as
-- `trial boolean NOT NULL`, so every Hibernate INSERT into plan_abonnement
-- (which now omits the column) failed at the PostgreSQL NOT NULL
-- constraint — manifesting as a 500 on POST /api/v1/plans in the admin
-- UI. The existing "Essai" row predates the entity change and is fine.
--
-- Trial-vs-paid is now carried by TypePlanAbonnement.trial (V11) at the
-- subscription-type level + Abonnement.statut = TRIAL at the subscription
-- level. The plan-level flag is no longer needed.
--
-- Idempotent: IF EXISTS guards against re-runs and against fresh DBs
-- where V1 might already have been hot-patched.

ALTER TABLE plan_abonnement
    DROP COLUMN IF EXISTS trial;
