package org.store.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtre HTTP qui trace chaque requête entrante / sortante avec son body, son status,
 * sa durée, masque les champs sensibles, et propage un requestId via MDC.
 */
@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    private static final List<String> SKIP_PATTERNS = List.of(
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    private final int maxPayloadLength;

    private static final List<String> SENSITIVE_FIELDS = List.of(
            "password", "accessToken", "refreshToken", "secret", "token"
    );

    private static final Pattern SENSITIVE_JSON_PATTERN = Pattern.compile(
            "(\"(?:" + String.join("|", SENSITIVE_FIELDS) + ")\"\\s*:\\s*)\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public HttpRequestLoggingFilter(org.store.property.LoggingProperties loggingProperties) {
        this.maxPayloadLength = loggingProperties.maxPayloadLength();
    }

    /**
     * Retourne true pour les paths techniques (actuator, swagger, openapi) qui ne doivent pas être logués.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Logue la requête entrante puis la réponse sortante (status + durée + bodies masqués),
     * en propageant un requestId via MDC pour toute la durée de la chaîne.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, maxPayloadLength * 2);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_REQUEST_ID, requestId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String remoteIp = resolveRemoteIp(request);
        long start = System.currentTimeMillis();

        log.info("→ {} {} from {}", method, path, remoteIp);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String requestBody = extractBody(
                    wrappedRequest.getContentAsByteArray(),
                    wrappedRequest.getCharacterEncoding(),
                    wrappedRequest.getContentType());
            String responseBody = extractBody(
                    wrappedResponse.getContentAsByteArray(),
                    wrappedResponse.getCharacterEncoding(),
                    wrappedResponse.getContentType());

            log.info("← {} {} {} in {}ms | request: {} | response: {}",
                    wrappedResponse.getStatus(), method, path, duration,
                    mask(requestBody), mask(responseBody));

            wrappedResponse.copyBodyToResponse();
            MDC.clear();
        }
    }

    /**
     * Décode un body binaire en String avec tronquage à maxPayloadLength
     * et remplace les contenus non textuels par [binary].
     */
    private String extractBody(byte[] content, String encoding, String contentType) {
        if (content == null || content.length == 0) {
            return "";
        }
        if (contentType != null && (
                contentType.startsWith("multipart/")
                        || contentType.startsWith("application/octet-stream")
                        || contentType.startsWith("image/")
                        || contentType.startsWith("video/")
                        || contentType.startsWith("audio/"))) {
            return "[binary]";
        }
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        String body = new String(content, charset);
        if (body.length() > maxPayloadLength) {
            return body.substring(0, maxPayloadLength) + "...[truncated]";
        }
        return body;
    }

    /**
     * Remplace par "***" la valeur des champs JSON sensibles (password, tokens, secrets).
     */
    private String mask(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        return SENSITIVE_JSON_PATTERN.matcher(body).replaceAll("$1\"***\"");
    }

    /**
     * Retourne l'IP réelle du client, en privilégiant le header X-Forwarded-For si présent.
     */
    private String resolveRemoteIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
