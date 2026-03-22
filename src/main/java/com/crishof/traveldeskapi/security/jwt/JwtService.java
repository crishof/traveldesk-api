package com.crishof.traveldeskapi.security.jwt;

import com.crishof.traveldeskapi.security.principal.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    private static final int CLOCK_SKEW_SECONDS = 30;

    private final long jwtExpiration;
    private final long refreshExpiration;
    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret_key}") String secretKeyValue,
                      @Value("${jwt.expiration}") long jwtExpiration,
                      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;
        this.signingKey = buildSigningKey(secretKeyValue);

        log.info("JWT access token expiration (ms): {}", jwtExpiration);
        log.info("JWT refresh token expiration (ms): {}", refreshExpiration);
    }

    public String generateAccessToken(SecurityUser user) {

        Map<String, Object> claims = Map.of(
                "uid", user.getId().toString(),
                "role", user.getRole().name(),
                "status", user.getStatus().name());
        return buildToken(claims, user.getUsername(), jwtExpiration);
    }

    public String generateRefreshToken(SecurityUser user) {
        Map<String, Object> claims = Map.of(
                "uid", user.getId().toString(),
                "type", "refresh");
        return buildToken(claims, user.getUsername(), refreshExpiration);
    }

    public String getUserName(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public Instant getExpiration(String token) {
        Date expiration = getClaim(token, Claims::getExpiration);
        return expiration.toInstant();
    }

    public boolean isTokenValid(String token) {
        try {
            getAllClaims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(getAllClaims(token));
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Instant now = Instant.now();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    private SecretKey buildSigningKey(String secretKeyValue) {
        if (secretKeyValue.isBlank()) {
            throw new IllegalStateException("JWT secret key must be configured");
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKeyValue);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT secret key must be a valid Base64-encoded value", ex);
        }
    }
}
