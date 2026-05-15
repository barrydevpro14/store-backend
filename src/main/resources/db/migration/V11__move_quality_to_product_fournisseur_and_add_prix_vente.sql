-- Deplacement de la quality du produit vers product_fournisseur.
-- Un meme produit peut etre fourni par un meme fournisseur en plusieurs qualites distinctes
-- (ex : Clou 10mm * Chine en qualite originale ET en qualite contrefacon).
-- Ajout du prix de vente courant sur product_fournisseur (renseigne lors de chaque achat).
-- Ajout du prix de vente snapshot sur ligne_commande_achat (tracabilite facture historique).

-- 1. product_fournisseur : ajout quality_id (nullable temporaire pour permettre le backfill).
ALTER TABLE product_fournisseur
    ADD COLUMN quality_id UUID;

ALTER TABLE product_fournisseur
    ADD CONSTRAINT fk_product_fournisseur_quality
        FOREIGN KEY (quality_id) REFERENCES quality (id);

-- 2. Backfill : copier la quality du produit parent vers chacun de ses product_fournisseur.
UPDATE product_fournisseur pf
SET quality_id = p.quality_id
FROM product p
WHERE pf.product_id = p.id;

-- 3. Promouvoir quality_id NOT NULL apres backfill.
ALTER TABLE product_fournisseur
    ALTER COLUMN quality_id SET NOT NULL;

-- 4. Unicite (product, fournisseur, quality) : un fournisseur peut livrer le meme produit
--    en plusieurs qualites distinctes, mais pas deux fois la meme combinaison.
ALTER TABLE product_fournisseur
    ADD CONSTRAINT uk_product_fournisseur_product_fournisseur_quality
        UNIQUE (product_id, fournisseur_id, quality_id);

CREATE INDEX idx_product_fournisseur_quality ON product_fournisseur (quality_id);

-- 5. product_fournisseur : ajout prix_vente courant.
--    Backfill = prix_achat (marge zero, a corriger par le manager via PUT /prix-vente).
ALTER TABLE product_fournisseur
    ADD COLUMN prix_vente NUMERIC(19,2);

UPDATE product_fournisseur
SET prix_vente = prix_achat
WHERE prix_vente IS NULL;

ALTER TABLE product_fournisseur
    ALTER COLUMN prix_vente SET NOT NULL;

-- 6. ligne_commande_achat : ajout prix_vente snapshot (tracabilite achat historique).
--    Backfill = prix_achat pour les lignes existantes.
ALTER TABLE ligne_commande_achat
    ADD COLUMN prix_vente NUMERIC(19,2);

UPDATE ligne_commande_achat
SET prix_vente = prix_achat
WHERE prix_vente IS NULL;

ALTER TABLE ligne_commande_achat
    ALTER COLUMN prix_vente SET NOT NULL;

-- 7. product : retrait de quality_id (la quality vit desormais sur product_fournisseur).
-- Drop dynamique de la FK : si la BD a ete baselinee sans V1 le nom de contrainte peut etre auto-genere par Hibernate.
DO $$
DECLARE
    cname TEXT;
BEGIN
    SELECT conname INTO cname
    FROM pg_constraint
    WHERE conrelid = 'product'::regclass
      AND contype = 'f'
      AND array_length(conkey, 1) = 1
      AND conkey[1] = (SELECT attnum FROM pg_attribute
                       WHERE attrelid = 'product'::regclass
                         AND attname = 'quality_id');
    IF cname IS NOT NULL THEN
        EXECUTE 'ALTER TABLE product DROP CONSTRAINT ' || quote_ident(cname);
    END IF;
END $$;

ALTER TABLE product
    DROP COLUMN IF EXISTS quality_id;
