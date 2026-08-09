-- V75 — Réduction de l'échelle des quantités et prix unitaires : NUMERIC(19,6) → NUMERIC(19,2)
--
-- Colonnes concernées : toutes celles migrées en NUMERIC(19,6) par V74
--   Quantités  : stock, entree_stock, sortie_stock, mouvement_stock,
--                ligne_commande_achat, ligne_commande_vente, ligne_inventaire
--   Prix units : entree_stock, sortie_stock, product_fournisseur,
--                ligne_commande_achat, ligne_commande_vente, ligne_inventaire
-- Colonnes non touchées : montants financiers déjà à NUMERIC(19,2)
--   (sortie_stock.marge, ligne_commande_vente.montant_total, tables paiement)

-- ── stock ─────────────────────────────────────────────────────────────────────
ALTER TABLE stock
    ALTER COLUMN quantite_disponible     TYPE NUMERIC(19,2) USING quantite_disponible::NUMERIC,
    ALTER COLUMN seuil_approvisionnement TYPE NUMERIC(19,2) USING seuil_approvisionnement::NUMERIC;

-- ── entree_stock ──────────────────────────────────────────────────────────────
ALTER TABLE entree_stock
    ALTER COLUMN quantite_initiale TYPE NUMERIC(19,2) USING quantite_initiale::NUMERIC,
    ALTER COLUMN quantite_restante TYPE NUMERIC(19,2) USING quantite_restante::NUMERIC,
    ALTER COLUMN prix_achat        TYPE NUMERIC(19,2) USING prix_achat::NUMERIC;

-- ── sortie_stock ──────────────────────────────────────────────────────────────
ALTER TABLE sortie_stock
    ALTER COLUMN quantite_sortie TYPE NUMERIC(19,2) USING quantite_sortie::NUMERIC,
    ALTER COLUMN prix_achat      TYPE NUMERIC(19,2) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente      TYPE NUMERIC(19,2) USING prix_vente::NUMERIC;

-- ── mouvement_stock ───────────────────────────────────────────────────────────
ALTER TABLE mouvement_stock
    ALTER COLUMN quantite    TYPE NUMERIC(19,2) USING quantite::NUMERIC,
    ALTER COLUMN stock_avant TYPE NUMERIC(19,2) USING stock_avant::NUMERIC,
    ALTER COLUMN stock_apres TYPE NUMERIC(19,2) USING stock_apres::NUMERIC;

-- ── product_fournisseur ───────────────────────────────────────────────────────
ALTER TABLE product_fournisseur
    ALTER COLUMN prix_achat TYPE NUMERIC(19,2) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente TYPE NUMERIC(19,2) USING prix_vente::NUMERIC;

-- ── ligne_commande_achat ──────────────────────────────────────────────────────
ALTER TABLE ligne_commande_achat
    ALTER COLUMN quantite   TYPE NUMERIC(19,2) USING quantite::NUMERIC,
    ALTER COLUMN prix_achat TYPE NUMERIC(19,2) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente TYPE NUMERIC(19,2) USING prix_vente::NUMERIC;

-- ── ligne_commande_vente ──────────────────────────────────────────────────────
ALTER TABLE ligne_commande_vente
    ALTER COLUMN quantite        TYPE NUMERIC(19,2) USING quantite::NUMERIC,
    ALTER COLUMN quantite_livree TYPE NUMERIC(19,2) USING quantite_livree::NUMERIC,
    ALTER COLUMN prix_unitaire   TYPE NUMERIC(19,2) USING prix_unitaire::NUMERIC;

-- ── ligne_inventaire ──────────────────────────────────────────────────────────
ALTER TABLE ligne_inventaire
    ALTER COLUMN quantite_theorique TYPE NUMERIC(19,2) USING quantite_theorique::NUMERIC,
    ALTER COLUMN quantite_reelle    TYPE NUMERIC(19,2) USING quantite_reelle::NUMERIC,
    ALTER COLUMN ecart              TYPE NUMERIC(19,2) USING ecart::NUMERIC,
    ALTER COLUMN prix_unitaire      TYPE NUMERIC(19,2) USING prix_unitaire::NUMERIC;
