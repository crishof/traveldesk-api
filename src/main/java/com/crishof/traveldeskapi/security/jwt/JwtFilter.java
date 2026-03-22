package com.crishof.traveldeskapi.security.jwt;

import com.crishof.traveldeskapi.security.principal.SecurityUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final SecurityUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        Optional<String> jwtToken = extractBearerToken(authHeader);
        if (jwtToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtToken.get();

        try {
            final String userEmail = jwtService.getUserName(jwt);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt) && userEmail.equalsIgnoreCase(userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            log.debug("JWT authentication failed for path {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.warn("Unexpected authentication error for path {}", request.getRequestURI(), ex);
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return Optional.empty();
        }

        String trimmedHeader = authHeader.trim();
        if (!trimmedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }

        String token = trimmedHeader.substring(7).trim();
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }
}
