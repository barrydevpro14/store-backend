-- V69 : Backfill plan_abonnement_id + statut — abonnements existants → plan trial
--
-- Contexte : V68 a ajouté plan_abonnement_id en nullable car les données
-- de type_plan_abonnement avaient été supprimées par V65 et ne pouvaient
-- pas servir à reconstruire la relation. Les abonnements antérieurs à V68
-- (essentiellement des TRIAL créés en dev) ont donc plan_abonnement_id = NULL
-- et un statut potentiellement incohérent.
--
-- Cette migration :
--   1. Rattache les abonnements sans plan au plan trial actif (trial = true).
--   2. Force leur statut à TRIAL (cohérence avec le plan assigné).
--   3. Passe plan_abonnement_id NOT NULL pour correspondre à
--      @ManyToOne(optional = false) / @JoinColumn(nullable = false).
--
-- Comportement selon l'état de la base :
--   • DB vierge (aucun plan trial)   → backfill ignoré, ALTER réussit (0 ligne).
--   • DB existante avec plan trial   → toutes les lignes null backfillées, ALTER réussit.
--   • DB existante sans plan trial   → ALTER échoue explicitement (lignes null restantes).

DO $$
DECLARE
    v_trial_plan_id uuid;
    v_updated       int;
BEGIN
    SELECT id INTO v_trial_plan_id
    FROM plan_abonnement
    WHERE trial = true
    LIMIT 1;

    IF v_trial_plan_id IS NULL THEN
        RAISE NOTICE 'V69 : aucun plan trial trouvé — backfill non nécessaire (base vierge).';
        RETURN;
    END IF;

    UPDATE abonnement
    SET plan_abonnement_id = v_trial_plan_id,
        statut             = 'TRIAL'
    WHERE plan_abonnement_id IS NULL;

    GET DIAGNOSTICS v_updated = ROW_COUNT;
    RAISE NOTICE 'V69 : % abonnement(s) rattaché(s) au plan trial (id = %) et statut → TRIAL.', v_updated, v_trial_plan_id;
END $$;

ALTER TABLE abonnement ALTER COLUMN plan_abonnement_id SET NOT NULL;
