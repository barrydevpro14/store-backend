package org.store.common.dto;

/**
 * Ligne brute extraite d'un fichier Excel d'import stock.
 * Tous les champs sont des String (la conversion typée se fait dans le service d'import).
 * {@code lineNumber} correspond au numéro de ligne dans le fichier (1 = entête, 2 = première donnée).
 * Ordre des colonnes : referenceProduit | nomProduit | categorie | qualite | quantite | prixAchat | prixVente | numeroLot | dateExpiration.
 */
public record ExcelEntreeStockRow(
        int lineNumber,
        String referenceProduit,
        String nomProduit,
        String categorie,
        String qualite,
        String quantite,
        String prixAchat,
        String prixVente,
        String numeroLot,
        String dateExpiration
) {
}
