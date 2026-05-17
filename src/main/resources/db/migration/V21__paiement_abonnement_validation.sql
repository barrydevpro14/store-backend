-- Ajout du workflow de validation manuelle des paiements d'abonnement.
-- Le propriétaire enregistre son paiement avec une preuve image ; un admin valide ou rejette.
-- À la validation, l'abonnement (statut=EN_ATTENTE) est activé (statut=ACTIF + dateDebut/dateFin calculés).

ALTER TABLE paiement_abonnement
    ADD COLUMN statut      VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
    ADD COLUMN preuve_id   UUID,
    ADD COLUMN motif_rejet TEXT;

ALTER TABLE paiement_abonnement
    ADD CONSTRAINT fk_paiement_abonnement_preuve
        FOREIGN KEY (preuve_id) REFERENCES piece_jointe (id);

CREATE INDEX idx_paiement_abonnement_statut        ON paiement_abonnement (statut);
CREATE INDEX idx_paiement_abonnement_abonnement_id ON paiement_abonnement (abonnement_id);
