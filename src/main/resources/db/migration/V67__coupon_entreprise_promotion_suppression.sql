-- V67: Coupon refonte + Promotion suppression
--
-- coupon           : supprime date_debut, date_fin (actif seul contrôle la validité)
--                    ajoute entreprise_id (null = coupon global, non-null = coupon ciblé)
-- utilisation_coupon : ajoute paiement_abonnement_id (FK vers la facture appliquée)
-- promotion        : table supprimée (remplacée par coupon global entreprise=null)

-- 1. Supprime les champs de fenêtre temporelle sur coupon
ALTER TABLE coupon DROP COLUMN IF EXISTS date_debut;
ALTER TABLE coupon DROP COLUMN IF EXISTS date_fin;

-- 2. Ajoute entreprise_id (nullable) sur coupon
ALTER TABLE coupon ADD COLUMN entreprise_id uuid;
ALTER TABLE coupon
    ADD CONSTRAINT fk_coupon_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

-- 3. Ajoute paiement_abonnement_id sur utilisation_coupon
ALTER TABLE utilisation_coupon ADD COLUMN paiement_abonnement_id uuid;
ALTER TABLE utilisation_coupon
    ADD CONSTRAINT fk_utilisation_coupon_paiement
        FOREIGN KEY (paiement_abonnement_id) REFERENCES paiement_abonnement(id);

-- 4. Supprime la table promotion (aucune autre table n'en dépend)
DROP TABLE IF EXISTS promotion;
