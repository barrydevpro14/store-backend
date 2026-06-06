-- ──────────────────────────────────────────────────────────────────
-- 1. Compute canonical id per (product_id, fournisseur_id, quality_id)
--    (the one with the smallest UUID — i.e. earliest created).
-- ──────────────────────────────────────────────────────────────────
CREATE TEMP TABLE pf_canonical AS
SELECT DISTINCT ON (product_id, fournisseur_id, quality_id)
       id AS canonical_id,
       product_id,
       fournisseur_id,
       quality_id
FROM product_fournisseur
ORDER BY product_id, fournisseur_id, quality_id, id;

-- Table of (dup_id → canonical_id) for every non-canonical row.
CREATE TEMP TABLE pf_duplicates AS
SELECT pf.id AS dup_id, c.canonical_id
FROM product_fournisseur pf
JOIN pf_canonical c USING (product_id, fournisseur_id, quality_id)
WHERE pf.id <> c.canonical_id;

-- ──────────────────────────────────────────────────────────────────
-- 2. Re-point all FK references to the canonical record.
-- ──────────────────────────────────────────────────────────────────
UPDATE ligne_commande_achat t
SET    product_fournisseur_id = d.canonical_id
FROM   pf_duplicates d
WHERE  t.product_fournisseur_id = d.dup_id;

UPDATE ligne_commande_vente t
SET    product_fournisseur_id = d.canonical_id
FROM   pf_duplicates d
WHERE  t.product_fournisseur_id = d.dup_id;

UPDATE entree_stock t
SET    product_fournisseur_id = d.canonical_id
FROM   pf_duplicates d
WHERE  t.product_fournisseur_id = d.dup_id;

UPDATE ligne_inventaire t
SET    product_fournisseur_id = d.canonical_id
FROM   pf_duplicates d
WHERE  t.product_fournisseur_id = d.dup_id;

-- stock table also carries product_fournisseur_id
UPDATE stock t
SET    product_fournisseur_id = d.canonical_id
FROM   pf_duplicates d
WHERE  t.product_fournisseur_id = d.dup_id;

-- ──────────────────────────────────────────────────────────────────
-- 3. Delete the now-unreferenced duplicate rows.
-- ──────────────────────────────────────────────────────────────────
DELETE FROM product_fournisseur pf
WHERE pf.id IN (SELECT dup_id FROM pf_duplicates);

-- ──────────────────────────────────────────────────────────────────
-- 4. Enforce uniqueness going forward.
-- ──────────────────────────────────────────────────────────────────
ALTER TABLE product_fournisseur
    ADD CONSTRAINT pf_product_fournisseur_quality_unique
    UNIQUE (product_id, fournisseur_id, quality_id);
