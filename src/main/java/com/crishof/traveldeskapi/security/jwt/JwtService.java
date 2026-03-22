package com.crishof.traveldeskapi.security.jwt;

import com.crishof.traveldeskapi.security.principal.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
        log.debug("Generating access token for user: {}", user.getUsername());

        Map<String, Object> claims = Map.of(
                "uid", user.getId().toString(),
                "role", user.getRole().name(),
                "status", user.getStatus().name());
        return buildToken(claims, user.getUsername(), jwtExpiration);
    }

    public String generateRefreshToken(SecurityUser user) {
        log.debug("Generating refresh token for user: {}", user.getUsername());
        Map<String, Object> claims = Map.of(
                "uid", user.getId().toString(),
                "type", "refresh");
        return buildToken(claims, user.getUsername(), refreshExpiration);
    }

    public String getUserName(String token) {
        log.debug("Getting username from token: {}", token);
        return getClaim(token, Claims::getSubject);
    }

    public Instant getExpiration(String token) {
        log.debug("Getting expiration from token: {}", token);
        Date expiration = getClaim(token, Claims::getExpiration);
        return expiration.toInstant();
    }

    public boolean isTokenValid(String token) {
        log.debug("Validating token: {}", token);
        try {
            log.debug("Getting claims from token: {}", token);
            getAllClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token expired: {}", ex.getMessage());
            return false;
        } catch (JwtException ex) {
            log.debug("Token invalid: {}", ex.getMessage());
            return false;
        }
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        log.debug("Getting claim from token: {}", token);
        return claimsResolver.apply(getAllClaims(token));
    }

    private Claims getAllClaims(String token) {
        log.debug("Getting all claims from token: {}", token);
        return Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        log.debug("Building token for subject: {}, expiration (ms): {}", subject, expirationMs);
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
        log.debug("Building signing key from value: {}", secretKeyValue);
        if (secretKeyValue.isBlank()) {
            log.error("JWT secret key is blank");
            throw new IllegalStateException("JWT secret key must be configured");
        }

        try {
            log.debug("Decoding secret key value");
            byte[] keyBytes = Decoders.BASE64.decode(secretKeyValue);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            log.error("JWT secret key is not a valid Base64-encoded value");
            throw new IllegalStateException("JWT secret key must be a valid Base64-encoded value", ex);
        }
    }
}
