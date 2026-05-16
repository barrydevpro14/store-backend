-- F-V3 : module Vente atomique.
-- 1. Table paiement_vente (symetrique a paiement_achat) : N paiements possibles par facture client.
-- 2. Lien ligne_commande_vente -> product_fournisseur : la vente cible une variante PF
--    (Clou 10mm Chine vs Maroc), avec un prixVente plancher = pf.prix_vente.

CREATE TABLE paiement_vente (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    montant         NUMERIC(19,2)        NOT NULL,
    date_paiement   DATE                 NOT NULL,
    moyen           VARCHAR(20)          NOT NULL,
    facture_id      UUID                 NOT NULL
);

ALTER TABLE paiement_vente
    ADD CONSTRAINT fk_paiement_vente_facture
        FOREIGN KEY (facture_id) REFERENCES facture_client (id);

CREATE INDEX idx_paiement_vente_facture ON paiement_vente (facture_id);

ALTER TABLE ligne_commande_vente
    ADD COLUMN product_fournisseur_id UUID;

ALTER TABLE ligne_commande_vente
    ADD CONSTRAINT fk_ligne_commande_vente_pf
        FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur (id);

CREATE INDEX idx_ligne_commande_vente_pf ON ligne_commande_vente (product_fournisseur_id);
