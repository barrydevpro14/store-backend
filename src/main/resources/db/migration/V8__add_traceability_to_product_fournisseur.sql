-- Tracabilite fournisseur enrichie : reference cote fournisseur + origine du produit (pays, marque).
-- Les colonnes product_id et fournisseur_id existent deja (V1) ; on les promeut NOT NULL ici.

ALTER TABLE product_fournisseur
    ALTER COLUMN product_id SET NOT NULL;

ALTER TABLE product_fournisseur
    ALTER COLUMN fournisseur_id SET NOT NULL;

ALTER TABLE product_fournisseur
    ADD COLUMN reference_fournisseur VARCHAR(100);

ALTER TABLE product_fournisseur
    ADD COLUMN origine VARCHAR(100);

CREATE INDEX idx_product_fournisseur_product ON product_fournisseur (product_id);
CREATE INDEX idx_product_fournisseur_fournisseur ON product_fournisseur (fournisseur_id);
