-- Scoping multi-tenant : chaque entreprise possede son propre catalogue de fournisseurs.

ALTER TABLE fournisseur
    ADD COLUMN entreprise_id UUID NOT NULL;

ALTER TABLE fournisseur
    ADD CONSTRAINT fk_fournisseur_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise (id);

CREATE INDEX idx_fournisseur_entreprise ON fournisseur (entreprise_id);
