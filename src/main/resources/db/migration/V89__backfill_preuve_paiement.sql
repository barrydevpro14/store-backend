-- Backfill one PreuvePaiement per existing paiement_abonnement that isn't a fresh FACTURE_GENEREE.
-- Statut mapping: VALIDE -> VALIDEE, REJETE -> REJETEE, EN_ATTENTE_VALIDATION -> EN_ATTENTE_VALIDATION.
INSERT INTO preuve_paiement (id, paiement_abonnement_id, date, moyen_id, reference_transaction, preuve_id, statut, motif_rejet, created_at, updated_at, created_by, updated_by)
SELECT
    gen_random_uuid(),
    pa.id,
    COALESCE(pa.date_paiement, pa.created_at::date),
    pa.moyen_id,
    pa.reference_transaction,
    pa.preuve_id,
    CASE pa.statut
        WHEN 'VALIDE' THEN 'VALIDEE'
        WHEN 'REJETE' THEN 'REJETEE'
        ELSE 'EN_ATTENTE_VALIDATION'
    END,
    pa.motif_rejet,
    pa.created_at,
    pa.updated_at,
    pa.created_by,
    pa.updated_by
FROM paiement_abonnement pa
WHERE pa.statut <> 'FACTURE_GENEREE'
  AND pa.statut <> 'EN_RETARD'
  AND pa.moyen_id IS NOT NULL
  AND pa.reference_transaction IS NOT NULL;

-- Factures that were REJETE or EN_ATTENTE_VALIDATION become FACTURE_GENEREE again — those
-- two statuts no longer exist at facture level in the new model.
-- Also convert any other statuses to FACTURE_GENEREE to ensure constraint compliance.
UPDATE paiement_abonnement
SET statut = 'FACTURE_GENEREE'
WHERE statut NOT IN ('FACTURE_GENEREE', 'EN_RETARD', 'VALIDE');
