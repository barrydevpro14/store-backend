package org.store.vente.application.pdf.renderer;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.common.service.IPdfService;
import org.store.country.domain.model.Country;
import org.store.entreprise.application.dto.EntrepriseSettingResponse;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.UniteMesure;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThermalInvoicePdfRendererTest {

    private static final Map<String, String> MESSAGES = Map.of(
            "pdf.vente.title", "FACTURE",
            "pdf.vente.client.anonyme", "Client anonyme",
            "pdf.vente.totals.totalHt", "Total HT",
            "pdf.vente.totals.soldeRestant", "Solde restant"
    );

    private ThermalInvoicePdfRenderer renderer;

    @BeforeEach
    void setUp() {
        IPdfService pdfService = mock(IPdfService.class);
        when(pdfService.msg(anyString())).thenAnswer(call -> MESSAGES.getOrDefault(call.getArgument(0, String.class), ""));
        when(pdfService.isNotBlank(any())).thenAnswer(call -> {
            String value = call.getArgument(0);
            return value != null && !value.isBlank();
        });
        when(pdfService.nullToEmpty(any())).thenAnswer(call -> {
            String value = call.getArgument(0);
            return value == null ? "" : value;
        });
        when(pdfService.formatAmount(any())).thenAnswer(call -> call.getArgument(0, BigDecimal.class).toPlainString());

        IEntrepriseSettingService entrepriseSettingService = mock(IEntrepriseSettingService.class);
        when(entrepriseSettingService.getMySettings()).thenReturn(new EntrepriseSettingResponse((String) null));

        IPaiementVenteService paiementVenteService = mock(IPaiementVenteService.class);
        when(paiementVenteService.findAllByFactureId(any())).thenReturn(List.of());

        renderer = new ThermalInvoicePdfRenderer(pdfService, paiementVenteService, entrepriseSettingService);
    }

    @Test
    void render_should_print_compact_header_and_magasin_footer_without_labels() throws Exception {
        Magasin magasin = sampleMagasin();
        FactureClient facture = sampleFacture(magasin);

        String text = extractText(renderer.render(facture, magasin, sampleThermalConfig()));

        assertThat(text)
                .contains("BARRY QUINCAILLERIE GENERALE")
                .contains("011709394")
                .contains("RC.SN.DKR.2024.A.50662")
                .contains("FACTURE FACT-2026-000056")
                .contains("Client anonyme")
                .contains("KEUR MASSAR NORD")
                .contains("+221773690065")
                .doesNotContain("NINEA :")
                .doesNotContain("RCCM :")
                .doesNotContain("CLIENT :")
                .doesNotContain("NUMÉRO")
                .doesNotContain("QUINCAILLERIE DE DETAIL")
                .doesNotContain("Sénégal")
                .doesNotContain("Boutique Centre");
    }

    private String extractText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    private Magasin sampleMagasin() {
        ActiviteEconomique activiteEconomique = new ActiviteEconomique();
        activiteEconomique.setLibelle("QUINCAILLERIE DE DETAIL");

        Country country = new Country();
        country.setName("Sénégal");

        Entreprise entreprise = new Entreprise();
        entreprise.setRaisonSociale("BARRY QUINCAILLERIE GENERALE");
        entreprise.setNinea("011709394");
        entreprise.setRccm("RC.SN.DKR.2024.A.50662");
        entreprise.setActiviteEconomique(activiteEconomique);
        entreprise.setCountry(country);

        Magasin magasin = new Magasin();
        magasin.setNom("Boutique Centre");
        magasin.setAdresse("KEUR MASSAR NORD ROUTE DES NIAYES");
        magasin.setTelephone("+221773690065");
        magasin.setEntreprise(entreprise);
        return magasin;
    }

    private FactureClient sampleFacture(Magasin magasin) {
        UniteMesure uniteMesure = new UniteMesure();
        uniteMesure.setSymbole("kg");

        Product product = new Product();
        product.setNom("Ciment");
        product.setUniteMesure(uniteMesure);

        ProductFournisseur productFournisseur = new ProductFournisseur();
        productFournisseur.setProduct(product);

        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(new BigDecimal("5"));
        ligne.setPrixUnitaire(new BigDecimal("100"));
        ligne.setMontantTotal(new BigDecimal("500"));

        CommandeVente commande = new CommandeVente();
        commande.setMagasin(magasin);
        commande.setLignes(List.of(ligne));

        FactureClient facture = new FactureClient();
        facture.setId(UUID.randomUUID());
        facture.setNumero("FACT-2026-000056");
        facture.setCommande(commande);
        facture.setMontantTotal(new BigDecimal("500"));
        facture.setMontantPaye(new BigDecimal("500"));
        return facture;
    }

    private PdfFormatConfig sampleThermalConfig() {
        PdfFormatConfig config = new PdfFormatConfig();
        config.setPageWidth(new BigDecimal("226"));
        config.setPageHeight(BigDecimal.ZERO);
        config.setMarginLeft(new BigDecimal("8"));
        config.setMarginRight(new BigDecimal("8"));
        config.setMarginTop(new BigDecimal("8"));
        config.setMarginBottom(new BigDecimal("8"));
        config.setFontSizeTitle(new BigDecimal("11"));
        config.setFontSizeNormal(new BigDecimal("9"));
        config.setFontSizeSmall(new BigDecimal("8"));
        return config;
    }
}
