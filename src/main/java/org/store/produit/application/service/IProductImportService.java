package org.store.produit.application.service;

import org.store.produit.application.dto.ProductImportRequest;
import org.store.produit.application.dto.ProductImportResult;

public interface IProductImportService {

    ProductImportResult importProducts(ProductImportRequest request);
}
