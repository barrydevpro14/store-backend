package org.store.produit.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.produit.application.dto.ImageMetadataResponse;
import org.store.common.service.IUploadFileService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.CategoryProductSummaryResponse;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.common.model.PieceJointe;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.application.service.impl.ProductServiceImpl;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductDomainService productDomainService;
    @Mock private ICategoryProductService categoryProductService;
    @Mock private IQualityService qualityService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private IUploadFileService uploadFileService;

    @InjectMocks
    private ProductServiceImpl service;

    private UUID entrepriseId;
    private UUID productId;
    private UUID categoryId;
    private UUID qualityId;
    private Entreprise entreprise;
    private CategoryProduct category;
    private Quality quality;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        qualityId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        category = new CategoryProduct();
        category.setId(categoryId);
        category.setLibelle("Pneus");
        category.setEntreprise(entreprise);

        quality = new Quality();
        quality.setId(qualityId);
        quality.setLibelle("Premium");
        quality.setEntreprise(entreprise);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, UUID.randomUUID(), "owner", "PROPRIETAIRE",
                List.of("PRODUCT_CREATE", "PRODUCT_READ"));
    }

    private Product sampleProduct(Entreprise ent) {
        Product p = new Product();
        p.setId(productId);
        p.setNom("Pneu 195/65 R15");
        p.setReference("PN-195-65-R15");
        p.setDescription("Pneu été");
        p.setCategoryProduct(category);
        p.setQuality(quality);
        p.setEntreprise(ent);
        return p;
    }

    @Test
    void create_should_persist_when_inputs_valid() {
        ProductRequest request = new ProductRequest("Pneu 195/65 R15", "PN-195-65-R15", "Pneu été", categoryId, qualityId);
        Product saved = sampleProduct(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.existsByReferenceAndEntrepriseId("PN-195-65-R15", entrepriseId)).thenReturn(false);
        when(categoryProductService.findById(categoryId)).thenReturn(category);
        when(categoryProductService.ensureBelongsToCurrentEntreprise(category)).thenReturn(category);
        when(qualityService.findById(qualityId)).thenReturn(quality);
        when(qualityService.ensureBelongsToCurrentEntreprise(quality)).thenReturn(quality);
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(productDomainService.create(request, category, quality, entreprise)).thenReturn(saved);

        ProductResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.reference()).isEqualTo("PN-195-65-R15");
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(response.category().id()).isEqualTo(categoryId);
        assertThat(response.category().libelle()).isEqualTo("Pneus");
        assertThat(response.quality().id()).isEqualTo(qualityId);
        assertThat(response.quality().libelle()).isEqualTo("Premium");
    }

    @Test
    void create_should_throw_when_reference_already_exists() {
        ProductRequest request = new ProductRequest("x", "PN-DUP", null, categoryId, qualityId);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.existsByReferenceAndEntrepriseId("PN-DUP", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(productDomainService, never()).create(any(), any(), any(), any());
    }

    @Test
    void create_should_throw_when_category_belongs_to_other_entreprise() {
        ProductRequest request = new ProductRequest("x", "PN-OK", null, categoryId, qualityId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.existsByReferenceAndEntrepriseId("PN-OK", entrepriseId)).thenReturn(false);
        when(categoryProductService.findById(categoryId)).thenReturn(category);
        when(categoryProductService.ensureBelongsToCurrentEntreprise(category))
                .thenThrow(new ForbiddenException("categoryProduct.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).create(any(), any(), any(), any());
    }

    @Test
    void create_should_throw_when_quality_belongs_to_other_entreprise() {
        ProductRequest request = new ProductRequest("x", "PN-OK", null, categoryId, qualityId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.existsByReferenceAndEntrepriseId("PN-OK", entrepriseId)).thenReturn(false);
        when(categoryProductService.findById(categoryId)).thenReturn(category);
        when(categoryProductService.ensureBelongsToCurrentEntreprise(category)).thenReturn(category);
        when(qualityService.findById(qualityId)).thenReturn(quality);
        when(qualityService.ensureBelongsToCurrentEntreprise(quality))
                .thenThrow(new ForbiddenException("quality.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).create(any(), any(), any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        Product product = sampleProduct(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        ProductResponse response = service.findResponseById(productId);

        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(productId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductResponse sample = new ProductResponse(productId, "Pneu", "PN-1", "desc",
                new CategoryProductSummaryResponse(categoryId, "Pneus"),
                new QualitySummaryResponse(qualityId, "Premium"),
                entrepriseId, null);
        Page<ProductResponse> page = new PageImpl<>(List.of(sample), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findResponsesByEntrepriseId(entrepriseId, pageable)).thenReturn(page);

        Page<ProductResponse> result = service.findAllByCurrentEntreprise(pageable);

        assertThat(result.getContent()).containsExactly(sample);
    }

    @Test
    void update_should_change_fields() {
        Product product = sampleProduct(entreprise);
        ProductRequest request = new ProductRequest("Nouveau nom", "PN-195-65-R15", "Nouvelle desc", categoryId, qualityId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(categoryProductService.findById(categoryId)).thenReturn(category);
        when(categoryProductService.ensureBelongsToCurrentEntreprise(category)).thenReturn(category);
        when(qualityService.findById(qualityId)).thenReturn(quality);
        when(qualityService.ensureBelongsToCurrentEntreprise(quality)).thenReturn(quality);
        when(productDomainService.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = service.update(productId, request);

        assertThat(response.nom()).isEqualTo("Nouveau nom");
        assertThat(response.description()).isEqualTo("Nouvelle desc");
    }

    @Test
    void update_should_skip_unicity_check_when_reference_unchanged() {
        Product product = sampleProduct(entreprise);
        ProductRequest request = new ProductRequest("x", "PN-195-65-R15", null, categoryId, qualityId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(categoryProductService.findById(categoryId)).thenReturn(category);
        when(categoryProductService.ensureBelongsToCurrentEntreprise(category)).thenReturn(category);
        when(qualityService.findById(qualityId)).thenReturn(quality);
        when(qualityService.ensureBelongsToCurrentEntreprise(quality)).thenReturn(quality);
        when(productDomainService.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(productId, request);

        verify(productDomainService, never()).existsByReferenceAndEntrepriseId(any(), any());
    }

    @Test
    void update_should_throw_when_new_reference_taken() {
        Product product = sampleProduct(entreprise);
        ProductRequest request = new ProductRequest("x", "PN-NEW", null, categoryId, qualityId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(productDomainService.existsByReferenceAndEntrepriseId("PN-NEW", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(productId, request))
                .isInstanceOf(UniqueResourceException.class);

        verify(productDomainService, never()).save(any());
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.update(productId, new ProductRequest("x", "y", null, categoryId, qualityId)))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_owned() {
        Product product = sampleProduct(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        service.delete(productId);

        verify(productDomainService).delete(product);
    }

    @Test
    void delete_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.delete(productId))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).delete(any(Product.class));
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_throw_when_other() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.ensureBelongsToCurrentEntreprise(foreign))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findResponseById_should_expose_image_url_when_set() {
        Product product = sampleProduct(entreprise);
        PieceJointe img = new PieceJointe();
        img.setId(UUID.randomUUID());
        product.setImagePrincipal(img);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        ProductResponse response = service.findResponseById(productId);

        assertThat(response.image()).isEqualTo("/api/v1/products/" + productId + "/image");
    }

    @Test
    void findResponseById_should_return_null_image_url_when_absent() {
        Product product = sampleProduct(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        ProductResponse response = service.findResponseById(productId);

        assertThat(response.image()).isNull();
    }

    @Test
    void ensureReferenceAvailable_should_throw_when_taken() {
        when(productDomainService.existsByReferenceAndEntrepriseId(eq("PN-1"), eq(entrepriseId))).thenReturn(true);

        assertThatThrownBy(() -> service.ensureReferenceAvailable("PN-1", entrepriseId))
                .isInstanceOf(UniqueResourceException.class);
    }

    @Test
    void uploadImagePrincipal_should_attach_image_when_owned() {
        Product product = sampleProduct(entreprise);
        MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        PieceJointe built = new PieceJointe();
        built.setId(UUID.randomUUID());
        Product saved = sampleProduct(entreprise);
        saved.setImagePrincipal(built);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(uploadFileService.buildImage(file)).thenReturn(built);
        when(productDomainService.setImagePrincipal(product, built)).thenReturn(saved);

        ProductResponse response = service.uploadImagePrincipal(productId, file);

        assertThat(response.image()).isEqualTo("/api/v1/products/" + productId + "/image");
    }

    @Test
    void uploadImagePrincipal_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);
        MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.uploadImagePrincipal(productId, file))
                .isInstanceOf(ForbiddenException.class);

        verify(uploadFileService, never()).buildImage(any());
        verify(productDomainService, never()).setImagePrincipal(any(), any());
    }

    @Test
    void deleteImagePrincipal_should_clear_image_when_owned() {
        Product product = sampleProduct(entreprise);
        PieceJointe existing = new PieceJointe();
        existing.setId(UUID.randomUUID());
        product.setImagePrincipal(existing);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        service.deleteImagePrincipal(productId);

        verify(productDomainService).setImagePrincipal(product, null);
    }

    @Test
    void deleteImagePrincipal_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.deleteImagePrincipal(productId))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).setImagePrincipal(any(), any());
    }

    @Test
    void uploadImages_should_append_and_return_ids() {
        Product product = sampleProduct(entreprise);
        MultipartFile file1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        MultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{2});
        List<MultipartFile> files = List.of(file1, file2);

        PieceJointe pj1 = new PieceJointe();
        UUID id1 = UUID.randomUUID();
        pj1.setId(id1);
        PieceJointe pj2 = new PieceJointe();
        UUID id2 = UUID.randomUUID();
        pj2.setId(id2);
        List<PieceJointe> built = List.of(pj1, pj2);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(uploadFileService.buildImages(files)).thenReturn(built);
        when(productDomainService.addImages(product, built)).thenReturn(product);

        List<UUID> result = service.uploadImages(productId, files);

        assertThat(result).containsExactly(id1, id2);
    }

    @Test
    void uploadImages_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);
        MultipartFile file = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.uploadImages(productId, List.of(file)))
                .isInstanceOf(ForbiddenException.class);

        verify(uploadFileService, never()).buildImages(any());
        verify(productDomainService, never()).addImages(any(), any());
    }

    @Test
    void getImagePrincipal_should_return_bytes_and_stored_content_type() {
        Product product = sampleProduct(entreprise);
        PieceJointe pj = new PieceJointe();
        byte[] payload = new byte[]{1, 2, 3};
        pj.setDocument(payload);
        pj.setContentType("image/png");
        product.setImagePrincipal(pj);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        ImageDownloadResponse response = service.getImagePrincipal(productId);

        assertThat(response.content()).isEqualTo(payload);
        assertThat(response.contentType()).isEqualTo("image/png");
    }

    @Test
    void getImagePrincipal_should_throw_when_absent() {
        Product product = sampleProduct(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        assertThatThrownBy(() -> service.getImagePrincipal(productId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void getImagePrincipal_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.getImagePrincipal(productId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getImage_should_return_bytes_and_stored_content_type() {
        Product product = sampleProduct(entreprise);
        PieceJointe img = new PieceJointe();
        UUID imgId = UUID.randomUUID();
        img.setId(imgId);
        byte[] payload = new byte[]{4, 5, 6};
        img.setDocument(payload);
        img.setContentType("image/jpeg");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(productDomainService.findImageInProduct(product, imgId)).thenReturn(java.util.Optional.of(img));

        ImageDownloadResponse response = service.getImage(productId, imgId);

        assertThat(response.content()).isEqualTo(payload);
        assertThat(response.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void getImage_should_throw_when_not_in_gallery() {
        Product product = sampleProduct(entreprise);
        UUID imgId = UUID.randomUUID();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(productDomainService.findImageInProduct(product, imgId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getImage(productId, imgId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void deleteImage_should_remove_image_when_present() {
        Product product = sampleProduct(entreprise);
        PieceJointe img = new PieceJointe();
        UUID imgId = UUID.randomUUID();
        img.setId(imgId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(productDomainService.findImageInProduct(product, imgId)).thenReturn(java.util.Optional.of(img));

        service.deleteImage(productId, imgId);

        verify(productDomainService).removeImage(product, img);
    }

    @Test
    void deleteImage_should_throw_when_image_absent() {
        Product product = sampleProduct(entreprise);
        UUID imgId = UUID.randomUUID();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);
        when(productDomainService.findImageInProduct(product, imgId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.deleteImage(productId, imgId))
                .isInstanceOf(EntityException.class);

        verify(productDomainService, never()).removeImage(any(), any());
    }

    @Test
    void deleteImage_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);
        UUID imgId = UUID.randomUUID();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.deleteImage(productId, imgId))
                .isInstanceOf(ForbiddenException.class);

        verify(productDomainService, never()).removeImage(any(), any());
    }

    @Test
    void listImages_should_return_metadata_for_each_image() {
        Product product = sampleProduct(entreprise);
        PieceJointe img1 = new PieceJointe();
        UUID id1 = UUID.randomUUID();
        img1.setId(id1);
        img1.setDate(java.time.LocalDate.of(2026, 5, 13));
        img1.setContentType("image/png");
        PieceJointe img2 = new PieceJointe();
        UUID id2 = UUID.randomUUID();
        img2.setId(id2);
        img2.setDate(java.time.LocalDate.of(2026, 5, 14));
        img2.setContentType("image/jpeg");
        product.getImages().addAll(List.of(img1, img2));

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        List<ImageMetadataResponse> result = service.listImages(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(id1);
        assertThat(result.get(0).contentType()).isEqualTo("image/png");
        assertThat(result.get(0).url()).isEqualTo("/api/v1/products/" + productId + "/images/" + id1);
        assertThat(result.get(1).id()).isEqualTo(id2);
        assertThat(result.get(1).url()).isEqualTo("/api/v1/products/" + productId + "/images/" + id2);
    }

    @Test
    void listImages_should_return_empty_when_no_images() {
        Product product = sampleProduct(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(product);

        List<ImageMetadataResponse> result = service.listImages(productId);

        assertThat(result).isEmpty();
    }

    @Test
    void listImages_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreign = sampleProduct(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productDomainService.findById(productId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.listImages(productId))
                .isInstanceOf(ForbiddenException.class);
    }
}
