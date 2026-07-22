-- V64: GIN trigram index for fournisseur.reference (LIKE '%term%')
-- nom/prenom/telephone are already covered by idx_person_* (V63).

CREATE INDEX IF NOT EXISTS idx_fournisseur_reference_trgm
    ON fournisseur USING GIN (LOWER(reference) gin_trgm_ops);
