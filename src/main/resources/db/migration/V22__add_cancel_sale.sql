-- Annulation de vente : ré-injection du stock + statut ANNULEE sur commande_vente / facture_client.
-- Le flag annulee sur sortie_stock préserve l'audit historique des consommations FIFO
-- tout en permettant aux reports (marges, caisse) d'exclure les sorties compensées.

ALTER TABLE commande_vente
    ADD COLUMN motif_annulation       VARCHAR(30),
    ADD COLUMN commentaire_annulation TEXT,
    ADD COLUMN date_annulation        TIMESTAMP;

ALTER TABLE sortie_stock
    ADD COLUMN annulee BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_sortie_stock_annulee ON sortie_stock (annulee);
