CREATE TABLE preuve_paiement (
    id                     UUID PRIMARY KEY,
    paiement_abonnement_id UUID NOT NULL REFERENCES paiement_abonnement(id),
    date                   DATE NOT NULL,
    moyen_id               UUID NOT NULL REFERENCES moyen_paiement(id),
    reference_transaction  VARCHAR(255) NOT NULL,
    preuve_id              UUID UNIQUE REFERENCES piece_jointe(id),
    statut                 VARCHAR(50) NOT NULL
        CHECK (statut IN ('EN_ATTENTE_VALIDATION', 'VALIDEE', 'REJETEE')),
    motif_rejet            TEXT,
    created_at             TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at             TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by             VARCHAR(255),
    updated_by             VARCHAR(255)
);

CREATE INDEX idx_preuve_paiement_paiement_abonnement_id ON preuve_paiement(paiement_abonnement_id);
