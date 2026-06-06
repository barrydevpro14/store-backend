package org.store.security.application.service.impl;

import org.store.security.application.service.IJwtService;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.store.property.JwtProperties;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.Claim;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtServiceImpl implements IJwtService {
    private final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtServiceImpl(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes());
    }

    @Override
    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.expiration().accessToken().toMillis());

        return Jwts.builder()
                .setSubject(principal.accountId().toString())
                .claim(Claim.USER.getKey(), principal.userId() != null ? principal.userId().toString() : null)
                .claim(Claim.ENTREPRISE.getKey(), principal.entrepriseId() != null ? principal.entrepriseId().toString() : null)
                .claim(Claim.MAGASIN.getKey(), principal.magasinId() != null ? principal.magasinId().toString() : null)
                .claim(Claim.USERNAME.getKey(), principal.username())
                .claim(Claim.CURRENCY.getKey(), principal.currency())
                .claim(Claim.COUNTRY_NAME.getKey(), principal.countryName())
                .claim(Claim.ROLE.getKey(), principal.role())
                .claim(Claim.PERMISSIONS.getKey(), principal.permissions())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = parseClaims(token);

        UUID accountId = UUID.fromString(claims.getSubject());
        UUID userId = parseUuid(claims.get(Claim.USER.getKey(), String.class));
        UUID entrepriseId = parseUuid(claims.get(Claim.ENTREPRISE.getKey(), String.class));
        UUID magasinId = parseUuid(claims.get(Claim.MAGASIN.getKey(), String.class));
        String username = claims.get(Claim.USERNAME.getKey(), String.class);
        String currency = claims.get(Claim.CURRENCY.getKey(), String.class);
        String countryName = claims.get(Claim.COUNTRY_NAME.getKey(), String.class);
        String role = claims.get(Claim.ROLE.getKey(), String.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get(Claim.PERMISSIONS.getKey(), List.class);

        return new UserPrincipal(accountId, userId, entrepriseId, magasinId, username, currency, countryName, role, permissions);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private UUID parseUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
