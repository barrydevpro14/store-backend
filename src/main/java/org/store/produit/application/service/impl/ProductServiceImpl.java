package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.EntityException;
import org.store.produit.application.dto.ImageMetadataResponse;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.service.ICategoryProductService;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

/**
 * Gère le CRUD des produits, scopé sur l'entreprise de l'utilisateur courant.
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements IProductService {

    private final ProductDomainService productDomainService;
    private final ICategoryProductService categoryProductService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final IUploadFileService uploadFileService;

    public ProductServiceImpl(ProductDomainService productDomainService,
                              ICategoryProductService categoryProductService,
                              IEntrepriseService entrepriseService,
                              ICurrentUserService currentUserService,
                              IUploadFileService uploadFileService) {
        this.productDomainService = productDomainService;
        this.categoryProductService = categoryProductService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.uploadFileService = uploadFileService;
    }

    /** Crée un produit après vérification de l'unicité de la référence et de l'appartenance de la catégorie à l'entreprise du caller. */
    @Override
    @Transactional
    public ProductResponse create(ProductRequest productRequest) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        ensureReferenceAvailable(productRequest.reference(), entrepriseId);
        CategoryProduct categoryProduct = categoryProductService.ensureBelongsToCurrentEntreprise(
                categoryProductService.findById(productRequest.categoryProductId()));
        Entreprise entreprise = entrepriseService.findById(entrepriseId);
        Product saved = productDomainService.create(productRequest, categoryProduct, entreprise);
        return new ProductResponse(saved);
    }

    /** Retourne le produit ou lève `EntityException`. */
    @Override
    public Product findById(UUID id) {
        return productDomainService.findById(id);
    }

    /** Retourne le produit en `Response` après vérification de l'appartenance à l'entreprise du caller. */
    @Override
    public ProductResponse findResponseById(UUID id) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        return new ProductResponse(product);
    }

    /** Liste paginée des produits de l'entreprise du caller. */
    @Override
    public Page<ProductResponse> findAllByCurrentEntreprise(Pageable pageable) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return productDomainService.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    /** Met à jour les champs du produit après contrôle d'appartenance, d'unicité (si reference changée) et de cohérence catégorie. */
    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest productRequest) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        if (!product.getReference().equals(productRequest.reference())) {
            ensureReferenceAvailable(productRequest.reference(), product.getEntreprise().getId());
        }
        CategoryProduct categoryProduct = categoryProductService.ensureBelongsToCurrentEntreprise(
                categoryProductService.findById(productRequest.categoryProductId()));
        product.setNom(productRequest.nom());
        product.setReference(productRequest.reference());
        product.setDescription(productRequest.description());
        product.setCategoryProduct(categoryProduct);
        return new ProductResponse(productDomainService.save(product));
    }

    /** Supprime le produit après contrôle d'appartenance à l'entreprise du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        productDomainService.delete(product);
    }

    /** Lève `ForbiddenException` si le produit n'appartient pas à l'entreprise du caller. */
    @Override
    public Product ensureBelongsToCurrentEntreprise(Product product) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!product.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("product.notOwned");
        }
        return product;
    }

    /** Lève `UniqueResourceException` si la référence est déjà utilisée dans l'entreprise. */
    @Override
    public void ensureReferenceAvailable(String reference, UUID entrepriseId) {
        if (productDomainService.existsByReferenceAndEntrepriseId(reference, entrepriseId)) {
            throw new UniqueResourceException("product.reference.alreadyExists", reference);
        }
    }

    /** Téléverse une image principale (remplace l'ancienne via orphanRemoval) après contrôle d'appartenance et validation MIME. */
    @Override
    @Transactional
    public ProductResponse uploadImagePrincipal(UUID id, MultipartFile file) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        PieceJointe pieceJointe = uploadFileService.buildImage(file);
        return new ProductResponse(productDomainService.setImagePrincipal(product, pieceJointe));
    }

    /** Supprime l'image principale d'un produit (idempotent, orphanRemoval supprime la `PieceJointe`). */
    @Override
    @Transactional
    public void deleteImagePrincipal(UUID id) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        productDomainService.setImagePrincipal(product, null);
    }

    /** Téléverse plusieurs images dans la galerie après contrôle d'appartenance et validation de chaque fichier. */
    @Override
    @Transactional
    public List<UUID> uploadImages(UUID id, List<MultipartFile> files) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        List<PieceJointe> built = uploadFileService.buildImages(files);
        productDomainService.addImages(product, built);
        return built.stream().map(PieceJointe::getId).toList();
    }

    /** Retourne le binaire et le content-type de l'image principale après contrôle d'appartenance. */
    @Override
    public ImageDownloadResponse getImagePrincipal(UUID id) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(id));
        PieceJointe imagePrincipal = product.getImagePrincipal();
        if (imagePrincipal == null) {
            throw new EntityException("product.image.notFound");
        }
        return new ImageDownloadResponse(imagePrincipal.getDocument(), imagePrincipal.getContentType());
    }

    /** Retourne le binaire et le content-type d'une image de la galerie après contrôle d'appartenance produit et galerie. */
    @Override
    public ImageDownloadResponse getImage(UUID productId, UUID imageId) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(productId));
        PieceJointe image = productDomainService.findImageInProduct(product, imageId)
                .orElseThrow(() -> new EntityException("product.image.galleryImageNotFound", imageId));
        return new ImageDownloadResponse(image.getDocument(), image.getContentType());
    }

    /** Supprime une image de la galerie après contrôle d'appartenance ; orphanRemoval purge la `PieceJointe`. */
    @Override
    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(productId));
        PieceJointe image = productDomainService.findImageInProduct(product, imageId)
                .orElseThrow(() -> new EntityException("product.image.galleryImageNotFound", imageId));
        productDomainService.removeImage(product, image);
    }

    /** Retourne les métadonnées des images de la galerie après contrôle d'appartenance. */
    @Override
    public List<ImageMetadataResponse> listImages(UUID productId) {
        Product product = ensureBelongsToCurrentEntreprise(productDomainService.findById(productId));
        return product.getImages().stream()
                .map(image -> new ImageMetadataResponse(image, productId))
                .toList();
    }
}
