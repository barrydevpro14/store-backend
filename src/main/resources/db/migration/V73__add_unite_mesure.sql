-- Création de la table des unités de mesure
CREATE TABLE unites_mesure (
    id                  UUID            PRIMARY KEY,
    code                VARCHAR(20)     NOT NULL UNIQUE,
    libelle             VARCHAR(100)    NOT NULL,
    symbole             VARCHAR(10)     NOT NULL,
    precision_affichage INTEGER         NOT NULL
);

-- Insertion des unités initiales
INSERT INTO unites_mesure (id, code, libelle, symbole, precision_affichage)
VALUES
    (gen_random_uuid(), 'PIECE',       'Pièce',       'pce', 0),
    (gen_random_uuid(), 'SAC',         'Sac',         'sac', 2),
    (gen_random_uuid(), 'KG',          'Kilogramme',  'kg',  2),
    (gen_random_uuid(), 'LITRE',       'Litre',       'L',   2),
    (gen_random_uuid(), 'METRE',       'Mètre',       'm',   2),
    (gen_random_uuid(), 'METRE_CARRE', 'Mètre carré', 'm²',  2);

-- Ajout de la colonne sur product (nullable pour le backfill)
ALTER TABLE product
    ADD COLUMN unite_mesure_id UUID REFERENCES unites_mesure(id);

-- Backfill : tous les produits existants → PIECE
UPDATE product
SET unite_mesure_id = (SELECT id FROM unites_mesure WHERE code = 'PIECE');

-- Passage en NOT NULL
ALTER TABLE product
    ALTER COLUMN unite_mesure_id SET NOT NULL;

-- Index FK
CREATE INDEX idx_product_unite_mesure ON product(unite_mesure_id);
