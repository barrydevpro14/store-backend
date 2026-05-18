-- Réception partielle d'achat : tracer la quantité déjà réceptionnée par ligne.
-- Permet plusieurs réceptions partielles (lots distincts) avant bascule en RECEPTIONNEE
-- lorsque toutes les lignes sont totalement reçues (quantiteRecue == quantite).

ALTER TABLE ligne_commande_achat
    ADD COLUMN quantite_recue INTEGER NOT NULL DEFAULT 0;
