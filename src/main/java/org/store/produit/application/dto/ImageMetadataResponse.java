package org.store.produit.application.dto;

import org.store.common.model.PieceJointe;

import java.time.LocalDate;
import java.util.UUID;

public record ImageMetadataResponse(
        UUID id,
        LocalDate date,
        String contentType,
        String url
) {
    public ImageMetadataResponse(PieceJointe pieceJointe, UUID productId) {
        this(
                pieceJointe.getId(),
                pieceJointe.getDate(),
                pieceJointe.getContentType(),
                "/api/v1/products/" + productId + "/images/" + pieceJointe.getId()
        );
    }
}
