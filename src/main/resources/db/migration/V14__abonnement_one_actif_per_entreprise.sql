-- V14: Enforce "at most one Abonnement with actif=true per entreprise" at the DB level.
--
-- Background : the validation flow (admin validates paiement → AbonnementDomainService.activate
-- flips the paid Abonnement to ACTIF + actif=true) left the sibling TRIAL row untouched with
-- actif=true. Result : an entreprise could carry TWO actif=true rows simultaneously (the freshly
-- ACTIF paid one + the still-running TRIAL). Combined with findCurrentByEntreprise's JPQL — which
-- matches both ACTIF and live TRIAL — this triggered IncorrectResultSizeDataAccessException on
-- every subsequent login + /me/current call, surfacing as a 500 ("Une erreur inattendue").
--
-- Two-part fix in this migration :
--
-- 1. DATA CLEANUP — for every entreprise that has more than one actif=true row, keep the most
--    "current" one (ACTIF wins ; otherwise the most-recently-created) and demote all others to
--    actif=false + statut=EXPIRE. statut=EXPIRE matches the V10 CHECK constraint.
--
-- 2. PARTIAL UNIQUE INDEX — enforces the invariant at the DB level so future bugs in the service
--    layer can't reintroduce duplicates. PostgreSQL partial index : only rows with actif=true
--    participate, so deactivated (actif=false) rows are free to coexist.
--
-- Companion application-layer change in AbonnementDomainService.activate() : before flipping a
-- paid Abonnement to actif=true, deactivate every sibling actif=true row on the same entreprise.

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY entreprise_id
            ORDER BY CASE WHEN statut = 'ACTIF' THEN 0 ELSE 1 END,
                     created_at DESC NULLS LAST,
                     id DESC
        ) AS rn
    FROM abonnement
    WHERE actif = true
)
UPDATE abonnement t
SET actif = false,
    statut = 'EXPIRE',
    updated_at = now()
FROM ranked
WHERE t.id = ranked.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX abonnement_one_actif_per_entreprise
    ON abonnement (entreprise_id)
    WHERE actif = true;
