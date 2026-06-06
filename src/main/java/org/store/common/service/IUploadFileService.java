package org.store.common.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.common.model.PieceJointe;

import java.util.List;

public interface IUploadFileService {

    /**
     * Valide qu'il s'agit d'une image (non vide, type MIME image autorisé) et construit une `PieceJointe` non persistée.
     */
    PieceJointe buildImage(MultipartFile file);

    /**
     * Valide une liste non vide de fichiers image et construit autant de `PieceJointe` non persistées (s'arrête au premier fichier invalide).
     */
    List<PieceJointe> buildImages(List<MultipartFile> files);
}
