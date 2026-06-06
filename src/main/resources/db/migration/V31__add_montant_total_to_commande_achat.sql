ALTER TABLE commande_achat
    ADD COLUMN montant_total NUMERIC(19, 2) NOT NULL DEFAULT 0;
