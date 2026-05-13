package org.store.common.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.property.UploadProperties;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service technique de validation et de construction de `PieceJointe` à partir d'un upload multipart.
 */
@Service
public class UploadFileServiceImpl implements IUploadFileService {

    private final UploadProperties uploadProperties;

    public UploadFileServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    /** Construit une `PieceJointe` à partir d'un upload image après validation MIME et non-vacuité. */
    @Override
    public PieceJointe buildImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadArgumentException("upload.file.empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.allowedImageTypes().contains(contentType.toLowerCase())) {
            throw new BadArgumentException("upload.file.invalidImageType", contentType);
        }
        try {
            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDocument(file.getBytes());
            pieceJointe.setDate(LocalDate.now());
            return pieceJointe;
        } catch (IOException e) {
            throw new BadArgumentException("upload.file.readFailed", e.getMessage());
        }
    }

    /** Construit une `PieceJointe` pour chaque fichier image après validation de la liste et de chaque élément. */
    @Override
    public List<PieceJointe> buildImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadArgumentException("upload.files.empty");
        }
        return files.stream().map(this::buildImage).toList();
    }
}
