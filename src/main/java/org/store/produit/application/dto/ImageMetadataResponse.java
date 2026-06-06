package org.store.produit.application.dto;

import org.store.common.model.PieceJointe;
import org.store.common.tools.DateHelper;

import java.util.UUID;

public record ImageMetadataResponse(
        UUID id,
        String date,
        String contentType,
        String url
) {
    public ImageMetadataResponse(PieceJointe pieceJointe, UUID productId) {
        this(
                pieceJointe.getId(),
                DateHelper.format(pieceJointe.getDate()),
                pieceJointe.getContentType(),
                "/api/v1/products/" + productId + "/images/" + pieceJointe.getId()
        );
    }
}
