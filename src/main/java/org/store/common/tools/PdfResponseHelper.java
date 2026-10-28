package org.store.common.tools;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Builds the HTTP response of a generated PDF downloaded as an attachment. */
public final class PdfResponseHelper {

    private PdfResponseHelper() {
    }

    public static ResponseEntity<byte[]> attachment(String fileName, byte[] pdfBytes) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
