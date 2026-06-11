-- quantite_recue a été retiré du modèle JPA. Sur les DBs qui l'ont encore
-- (NOT NULL sans DEFAULT), Hibernate échoue à l'INSERT car la colonne est absente
-- du payload. On la supprime définitivement.
ALTER TABLE ligne_commande_achat DROP COLUMN IF EXISTS quantite_recue;
