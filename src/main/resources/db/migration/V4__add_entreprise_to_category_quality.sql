-- Scoping multi-tenant : chaque entreprise possede ses propres categories et qualites de produit.

ALTER TABLE category_product
    ADD COLUMN entreprise_id UUID NOT NULL;

ALTER TABLE category_product
    ADD CONSTRAINT fk_category_product_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise (id);

CREATE INDEX idx_category_product_entreprise ON category_product (entreprise_id);

ALTER TABLE quality
    ADD COLUMN entreprise_id UUID NOT NULL;

ALTER TABLE quality
    ADD CONSTRAINT fk_quality_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise (id);

CREATE INDEX idx_quality_entreprise ON quality (entreprise_id);
