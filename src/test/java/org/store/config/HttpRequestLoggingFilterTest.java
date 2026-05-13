package org.store.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLoggingFilterTest {

    private HttpRequestLoggingFilter filter;
    private Logger filterLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        filter = new HttpRequestLoggingFilter();
        filterLogger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        filterLogger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void should_put_requestId_in_mdc_during_chain_and_clear_after() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/produits");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] capturedDuringChain = new String[1];
        FilterChain chain = (req, res) -> capturedDuringChain[0] = MDC.get(HttpRequestLoggingFilter.MDC_REQUEST_ID);

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedDuringChain[0]).isNotBlank();
        assertThat(MDC.get(HttpRequestLoggingFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void should_log_inbound_and_outbound_with_status_and_duration() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/produits/42");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilterInternal(request, response, new MockFilterChain());

        List<ILoggingEvent> infos = appender.list.stream().filter(e -> e.getLevel() == Level.INFO).toList();
        assertThat(infos).hasSize(2);
        assertThat(infos.get(0).getFormattedMessage()).contains("→ GET /api/v1/produits/42 from 10.0.0.5");
        assertThat(infos.get(1).getFormattedMessage())
                .contains("← 200 GET /api/v1/produits/42")
                .contains("ms");
    }

    @Test
    void should_mask_password_field_in_request_body() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent("{\"username\":\"alice\",\"password\":\"sup3rs3cret\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Pour que ContentCachingRequestWrapper mémorise le body, le servlet doit le lire :
        FilterChain readingChain = (req, res) -> req.getInputStream().readAllBytes();

        filter.doFilterInternal(request, response, readingChain);

        String outboundLog = appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.startsWith("←"))
                .findFirst()
                .orElseThrow();
        assertThat(outboundLog).contains("\"password\":\"***\"");
        assertThat(outboundLog).doesNotContain("sup3rs3cret");
        assertThat(outboundLog).contains("\"username\":\"alice\"");
    }

    @Test
    void should_mask_accessToken_and_refreshToken_in_response_body() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"accessToken\":\"eyJabc\",\"refreshToken\":\"r-uuid\"}");
        };

        filter.doFilterInternal(request, response, chain);

        String outboundLog = appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.startsWith("←"))
                .findFirst()
                .orElseThrow();
        assertThat(outboundLog).contains("\"accessToken\":\"***\"");
        assertThat(outboundLog).contains("\"refreshToken\":\"***\"");
        assertThat(outboundLog).doesNotContain("eyJabc").doesNotContain("r-uuid");
    }

    @Test
    void should_skip_actuator_and_swagger_paths() {
        MockHttpServletRequest actuator = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletRequest swagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletRequest openapi = new MockHttpServletRequest("GET", "/v3/api-docs/swagger-config");
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/v1/produits");

        assertThat(filter.shouldNotFilter(actuator)).isTrue();
        assertThat(filter.shouldNotFilter(swagger)).isTrue();
        assertThat(filter.shouldNotFilter(openapi)).isTrue();
        assertThat(filter.shouldNotFilter(api)).isFalse();
    }

    @Test
    void should_prefer_x_forwarded_for_header_when_present() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        String inboundLog = appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.startsWith("→"))
                .findFirst()
                .orElseThrow();
        assertThat(inboundLog).contains("from 203.0.113.42");
    }
}
