UPDATE commande_vente cv
SET statut = 'CLOTURE'
FROM facture_client fc
WHERE fc.commande_id = cv.id
  AND fc.statut = 'PAYEE'
  AND cv.statut = 'VALIDATE';
