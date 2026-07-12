-- Add per-line delivery tracking on sale lines.
-- quantite_livree defaults to the ordered quantity + livraison_statut to LIVREE
-- for existing rows (a sale that is already validated in DB was, by convention,
-- fully delivered).

ALTER TABLE ligne_commande_vente
    ADD COLUMN quantite_livree INTEGER,
    ADD COLUMN livraison_statut VARCHAR(32);

UPDATE ligne_commande_vente
   SET quantite_livree  = quantite,
       livraison_statut = 'LIVREE';

ALTER TABLE ligne_commande_vente
    ALTER COLUMN quantite_livree SET NOT NULL,
    ALTER COLUMN livraison_statut SET NOT NULL;

ALTER TABLE ligne_commande_vente
    ADD CONSTRAINT ligne_commande_vente_livraison_statut_check
        CHECK (livraison_statut IN ('LIVREE', 'NON_LIVREE', 'PARTIELLEMENT_LIVREE')),
    ADD CONSTRAINT ligne_commande_vente_quantite_livree_check
        CHECK (quantite_livree >= 0 AND quantite_livree <= quantite);
