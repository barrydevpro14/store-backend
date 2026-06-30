-- Enforce per-entreprise uniqueness of (reference, nom) on product.
-- The previous (entreprise_id, reference) app-level check was stricter,
-- so no in-place dedup is required: existing data is already compatible.
ALTER TABLE product
    ADD CONSTRAINT uk_product_entreprise_reference_nom
    UNIQUE (entreprise_id, reference, nom);
