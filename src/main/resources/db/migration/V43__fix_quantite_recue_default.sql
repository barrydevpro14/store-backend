-- Adds DEFAULT 0 to quantite_recue so Hibernate INSERT (which omits the column)
-- doesn't fail on fresh deployments where V1 created the column without a default.
ALTER TABLE ligne_commande_achat ALTER COLUMN quantite_recue SET DEFAULT 0;
