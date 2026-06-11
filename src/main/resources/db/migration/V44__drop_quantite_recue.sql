-- quantite_recue a été retiré du modèle JPA. On supprime la colonne
-- pour aligner le schéma DB avec l'entité LigneCommandeAchat.
ALTER TABLE ligne_commande_achat DROP COLUMN IF EXISTS quantite_recue;
