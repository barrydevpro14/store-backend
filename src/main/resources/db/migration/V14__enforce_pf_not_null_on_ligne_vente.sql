-- F-V3 durcissement : product_fournisseur_id devient obligatoire sur ligne_commande_vente.
-- La V12 avait introduit la colonne nullable pour permettre la transition ; toutes les
-- lignes creees via VenteServiceImpl ont systematiquement un PF set, donc le NOT NULL
-- s'applique sans backfill en pratique. Le bloc DELETE garde-fou protege le cas d'une
-- ligne orpheline en BDD historique (qui serait de toute facon inutilisable).

DELETE FROM ligne_commande_vente WHERE product_fournisseur_id IS NULL;

ALTER TABLE ligne_commande_vente
    ALTER COLUMN product_fournisseur_id SET NOT NULL;
