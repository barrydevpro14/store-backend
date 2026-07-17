package org.store.produit.application.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.produit.application.dto.ProductImportRequest;
import org.store.produit.application.dto.ProductImportResult;

public interface IProductImportService {

    ProductImportResult importProducts(ProductImportRequest productImportRequest);

    ProductImportResult importFromFile(MultipartFile multipartFile);
}
