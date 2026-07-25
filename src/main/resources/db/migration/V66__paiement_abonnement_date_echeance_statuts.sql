-- V66: PaiementAbonnement refonte — date_echeance + nouveaux statuts
--
-- paiement_abonnement : ajoute date_echeance (date limite de paiement = dateFin du cycle)
--                       étend le CHECK statut : FACTURE_GENEREE + EN_RETARD

-- 1. Étend la contrainte CHECK statut
ALTER TABLE paiement_abonnement
    DROP CONSTRAINT IF EXISTS paiement_abonnement_statut_check;
ALTER TABLE paiement_abonnement
    ADD CONSTRAINT paiement_abonnement_statut_check
        CHECK (statut IN (
            'EN_ATTENTE_VALIDATION',
            'VALIDE',
            'REJETE',
            'FACTURE_GENEREE',
            'EN_RETARD'
        ));

-- 2. Ajoute date_echeance (nullable — les factures existantes n'en ont pas)
ALTER TABLE paiement_abonnement ADD COLUMN date_echeance date;
