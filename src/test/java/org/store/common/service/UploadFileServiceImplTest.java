package org.store.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.model.PieceJointe;
import org.store.common.service.impl.UploadFileServiceImpl;
import org.store.property.UploadProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadFileServiceImplTest {

    private final UploadFileServiceImpl service = new UploadFileServiceImpl(
            new UploadProperties(Set.of(
                    "image/jpeg", "image/png", "image/webp", "image/gif"
            ))
    );

    @Test
    void buildImage_should_return_pieceJointe_with_bytes_date_and_content_type() {
        byte[] payload = new byte[]{1, 2, 3, 4};
        MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", payload);

        PieceJointe pieceJointe = service.buildImage(file);

        assertThat(pieceJointe.getDocument()).isEqualTo(payload);
        assertThat(pieceJointe.getDate()).isEqualTo(LocalDate.now());
        assertThat(pieceJointe.getContentType()).isEqualTo("image/png");
    }

    @Test
    void buildImage_should_accept_jpeg_webp_gif() {
        for (String contentType : new String[]{"image/jpeg", "image/webp", "image/gif"}) {
            MultipartFile file = new MockMultipartFile("file", "img", contentType, new byte[]{1});
            PieceJointe pieceJointe = service.buildImage(file);
            assertThat(pieceJointe.getContentType()).isEqualTo(contentType);
        }
    }

    @Test
    void buildImage_should_throw_when_null() {
        assertThatThrownBy(() -> service.buildImage(null))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void buildImage_should_throw_when_empty() {
        MultipartFile file = new MockMultipartFile("file", "img.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.buildImage(file))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void buildImage_should_throw_when_content_type_missing() {
        MultipartFile file = new MockMultipartFile("file", "img.bin", null, new byte[]{1});

        assertThatThrownBy(() -> service.buildImage(file))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void buildImage_should_throw_when_content_type_not_image() {
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.buildImage(file))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void buildImages_should_return_one_pieceJointe_per_file() {
        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "a.png", "image/png", new byte[]{1}),
                new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{2, 3})
        );

        List<PieceJointe> result = service.buildImages(files);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDocument()).isEqualTo(new byte[]{1});
        assertThat(result.get(1).getDocument()).isEqualTo(new byte[]{2, 3});
    }

    @Test
    void buildImages_should_throw_when_list_null_or_empty() {
        assertThatThrownBy(() -> service.buildImages(null))
                .isInstanceOf(BadArgumentException.class);

        assertThatThrownBy(() -> service.buildImages(List.of()))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void buildImages_should_stop_at_first_invalid_file() {
        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "ok.png", "image/png", new byte[]{1}),
                new MockMultipartFile("files", "bad.pdf", "application/pdf", new byte[]{2})
        );

        assertThatThrownBy(() -> service.buildImages(files))
                .isInstanceOf(BadArgumentException.class);
    }
}
