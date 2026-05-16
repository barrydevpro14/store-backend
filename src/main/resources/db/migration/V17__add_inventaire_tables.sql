-- Inventaire physique : permet au magasinier de compter les pieces physiques
-- d'un magasin pour chaque variante (ProductFournisseur), comparer au stock
-- theorique (somme des EntreeStock actifs), et regulariser les ecarts via les
-- ajustements stock existants (F-stock-7) lors de la cloture.

CREATE TABLE inventaire (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    magasin_id      UUID                 NOT NULL,
    statut          VARCHAR(20)          NOT NULL,
    date            DATE                 NOT NULL,
    date_validation TIMESTAMP
);

ALTER TABLE inventaire
    ADD CONSTRAINT fk_inventaire_magasin
        FOREIGN KEY (magasin_id) REFERENCES magasin (id);

CREATE INDEX idx_inventaire_magasin ON inventaire (magasin_id);
CREATE INDEX idx_inventaire_statut ON inventaire (statut);

CREATE TABLE ligne_inventaire (
    id                      UUID PRIMARY KEY,
    inventaire_id           UUID    NOT NULL,
    product_fournisseur_id  UUID    NOT NULL,
    quantite_theorique      INTEGER NOT NULL,
    quantite_reelle         INTEGER NOT NULL,
    ecart                   INTEGER NOT NULL,
    UNIQUE (inventaire_id, product_fournisseur_id)
);

ALTER TABLE ligne_inventaire
    ADD CONSTRAINT fk_ligne_inventaire_inventaire
        FOREIGN KEY (inventaire_id) REFERENCES inventaire (id) ON DELETE CASCADE;

ALTER TABLE ligne_inventaire
    ADD CONSTRAINT fk_ligne_inventaire_product_fournisseur
        FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur (id);

CREATE INDEX idx_ligne_inventaire_inventaire ON ligne_inventaire (inventaire_id);
