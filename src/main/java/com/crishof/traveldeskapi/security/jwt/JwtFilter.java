package com.crishof.traveldeskapi.security.jwt;

import com.crishof.traveldeskapi.security.principal.SecurityUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
        log.debug("JWT filter processing request for path {}", request.getRequestURI());
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            log.debug("JWT filter skipping processing for path {}: no JWT token found", request.getRequestURI());
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.getUserName(jwt);
            log.debug("JWT filter processing request for path {}: user email {}", request.getRequestURI(), userEmail);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("JWT filter processing request for path {}: no authentication found", request.getRequestURI());
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt) && userEmail.equals(userDetails.getUsername())) {
                    log.debug("JWT filter processing request for path {}: authentication successful", request.getRequestURI());
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("JWT filter processing request for path {}: authentication set", request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            log.warn("JWT authentication failed for path {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        log.debug("JWT filter processing request for path {}: continuing filter chain", request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}
