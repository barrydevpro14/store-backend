-- V63: GIN trigram indexes for person search fields (LIKE '%term%')
-- nom/prenom/telephone/email live in the `person` table (JOINED inheritance),
-- shared by Client and Fournisseur subtypes.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_person_nom_trgm
    ON person USING GIN (LOWER(nom) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_person_prenom_trgm
    ON person USING GIN (LOWER(prenom) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_person_telephone_trgm
    ON person USING GIN (LOWER(telephone) gin_trgm_ops);
