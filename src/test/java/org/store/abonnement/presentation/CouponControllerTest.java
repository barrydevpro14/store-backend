package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponControllerTest {

    private MockMvc mockMvc;
    private ICouponService couponService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID couponId;

    @BeforeEach
    void setUp() {
        couponService = mock(ICouponService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CouponController(couponService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        couponId = UUID.randomUUID();
    }

    private CouponRequest validBody() {
        return new CouponRequest(
                "PROMO10", null, "POURCENTAGE",
                new BigDecimal("10"), 100,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, null);
    }

    private CouponResponse sample() {
        return new CouponResponse(couponId, "PROMO10", null,
                ReductionType.POURCENTAGE, new BigDecimal("10"),
                100, 0,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, null);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(couponService.create(any(CouponRequest.class))).thenReturn(sample());

        mockMvc.perform(post(CouponController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PROMO10"));
    }

    @Test
    void should_return_400_when_code_blank() throws Exception {
        CouponRequest body = new CouponRequest(
                "", null, "POURCENTAGE", new BigDecimal("10"), 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                true, null);

        mockMvc.perform(post(CouponController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_dateFin_null() throws Exception {
        String json = """
                { "code":"X", "reductionType":"POURCENTAGE", "valeurReduction":10,
                  "nombreUtilisationsMax":10, "dateDebut":"2026-01-01", "dateFin":null,
                  "actif":true, "planId":null }
                """;

        mockMvc.perform(post(CouponController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<CouponResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(couponService.findAll(any(CouponFilter.class))).thenReturn(page);

        mockMvc.perform(get(CouponController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(couponService.findResponseById(eq(couponId))).thenReturn(sample());

        mockMvc.perform(get(CouponController.BASE_PATH + "/" + couponId))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        when(couponService.update(eq(couponId), any(CouponRequest.class))).thenReturn(sample());

        mockMvc.perform(put(CouponController.BASE_PATH + "/" + couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_activated() throws Exception {
        when(couponService.activate(eq(couponId))).thenReturn(sample());

        mockMvc.perform(patch(CouponController.BASE_PATH + "/" + couponId + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(CouponController.BASE_PATH + "/" + couponId))
                .andExpect(status().isNoContent());

        verify(couponService).delete(couponId);
    }
}
