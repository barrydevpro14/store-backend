-- V74 — Migration Integer → NUMERIC(19,6) for quantity and unit-price fields
--
-- Quantities: stock.quantite_disponible, entree_stock.{quantite_initiale, quantite_restante},
--             sortie_stock.quantite_sortie, mouvement_stock.{quantite, stock_avant, stock_apres},
--             ligne_commande_vente.{quantite, quantite_livree},
--             ligne_commande_achat.quantite,
--             ligne_inventaire.{quantite_theorique, quantite_reelle, ecart}
--             stock.seuil_approvisionnement (compared against quantite_disponible)
--
-- Unit prices: entree_stock.prix_achat, sortie_stock.{prix_achat, prix_vente},
--              product_fournisseur.{prix_achat, prix_vente},
--              ligne_commande_achat.{prix_achat, prix_vente},
--              ligne_commande_vente.prix_unitaire,
--              ligne_inventaire.prix_unitaire
--
-- Amounts that stay NUMERIC(19,2): sortie_stock.marge, ligne_commande_vente.montant_total,
--                                   all paiement tables.

-- ── stock ────────────────────────────────────────────────────────────────────
ALTER TABLE stock
    ALTER COLUMN quantite_disponible     TYPE NUMERIC(19,6) USING quantite_disponible::NUMERIC,
    ALTER COLUMN seuil_approvisionnement TYPE NUMERIC(19,6) USING seuil_approvisionnement::NUMERIC;

-- ── entree_stock ─────────────────────────────────────────────────────────────
ALTER TABLE entree_stock
    ALTER COLUMN quantite_initiale TYPE NUMERIC(19,6) USING quantite_initiale::NUMERIC,
    ALTER COLUMN quantite_restante TYPE NUMERIC(19,6) USING quantite_restante::NUMERIC,
    ALTER COLUMN prix_achat        TYPE NUMERIC(19,6) USING prix_achat::NUMERIC;

-- ── sortie_stock ──────────────────────────────────────────────────────────────
ALTER TABLE sortie_stock
    ALTER COLUMN quantite_sortie TYPE NUMERIC(19,6) USING quantite_sortie::NUMERIC,
    ALTER COLUMN prix_achat      TYPE NUMERIC(19,6) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente      TYPE NUMERIC(19,6) USING prix_vente::NUMERIC;
-- marge = montant financier par sortie → reste NUMERIC(19,2)

-- ── mouvement_stock ───────────────────────────────────────────────────────────
ALTER TABLE mouvement_stock
    ALTER COLUMN quantite    TYPE NUMERIC(19,6) USING quantite::NUMERIC,
    ALTER COLUMN stock_avant TYPE NUMERIC(19,6) USING stock_avant::NUMERIC,
    ALTER COLUMN stock_apres TYPE NUMERIC(19,6) USING stock_apres::NUMERIC;

-- ── product_fournisseur ───────────────────────────────────────────────────────
ALTER TABLE product_fournisseur
    ALTER COLUMN prix_achat TYPE NUMERIC(19,6) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente TYPE NUMERIC(19,6) USING prix_vente::NUMERIC;

-- ── ligne_commande_achat ──────────────────────────────────────────────────────
ALTER TABLE ligne_commande_achat
    ALTER COLUMN quantite   TYPE NUMERIC(19,6) USING quantite::NUMERIC,
    ALTER COLUMN prix_achat TYPE NUMERIC(19,6) USING prix_achat::NUMERIC,
    ALTER COLUMN prix_vente TYPE NUMERIC(19,6) USING prix_vente::NUMERIC;

-- ── ligne_commande_vente ──────────────────────────────────────────────────────
ALTER TABLE ligne_commande_vente
    ALTER COLUMN quantite        TYPE NUMERIC(19,6) USING quantite::NUMERIC,
    ALTER COLUMN quantite_livree TYPE NUMERIC(19,6) USING quantite_livree::NUMERIC,
    ALTER COLUMN prix_unitaire   TYPE NUMERIC(19,6) USING prix_unitaire::NUMERIC;
-- montant_total = montant financier de la ligne → reste NUMERIC(19,2)

-- ── ligne_inventaire ──────────────────────────────────────────────────────────
ALTER TABLE ligne_inventaire
    ALTER COLUMN quantite_theorique TYPE NUMERIC(19,6) USING quantite_theorique::NUMERIC,
    ALTER COLUMN quantite_reelle    TYPE NUMERIC(19,6) USING quantite_reelle::NUMERIC,
    ALTER COLUMN ecart              TYPE NUMERIC(19,6) USING ecart::NUMERIC,
    ALTER COLUMN prix_unitaire      TYPE NUMERIC(19,6) USING prix_unitaire::NUMERIC;
