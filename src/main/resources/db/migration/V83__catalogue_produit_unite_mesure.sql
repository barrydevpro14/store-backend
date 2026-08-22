ALTER TABLE catalogue_produit
    ADD COLUMN IF NOT EXISTS unite_mesure VARCHAR(20);

UPDATE catalogue_produit
SET unite_mesure = 'PIECE'
WHERE unite_mesure IS NULL;
