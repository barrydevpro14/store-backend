-- Annulation d'achat : ré-extraction du stock + statut ANNULEE sur commande_achat / facture_achat.
-- Le flag annulee sur entree_stock préserve l'audit historique des lots reçus tout en permettant
-- aux reports (valorisation stock, lots expirants) d'exclure les lots retournés au fournisseur.

ALTER TABLE commande_achat
    ADD COLUMN motif_annulation       VARCHAR(30),
    ADD COLUMN commentaire_annulation TEXT,
    ADD COLUMN date_annulation        TIMESTAMP;

ALTER TABLE entree_stock
    ADD COLUMN annulee BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_entree_stock_annulee ON entree_stock (annulee);
