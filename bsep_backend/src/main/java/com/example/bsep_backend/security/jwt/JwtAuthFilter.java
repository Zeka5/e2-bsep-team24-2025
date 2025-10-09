package com.example.bsep_backend.security.jwt;

import com.example.bsep_backend.security.user.CustomUserDetailsService;
import com.example.bsep_backend.service.intr.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if(token!= null){
            String username = jwtUtils.getUsernameFromToken(token);

            if(StringUtils.hasText(username) && jwtUtils.isTokenValid(token)) {
                // Check if session is active
                String sessionId = jwtUtils.getSessionIdFromToken(token);
                if(sessionId != null && !userSessionService.isSessionActive(sessionId)) {
                    // Session has been revoked, don't authenticate - just continue without setting authentication
                    // Spring Security will handle this as unauthorized
                    filterChain.doFilter(request, response);
                    return;
                }

                // Update session activity
                if(sessionId != null) {
                    userSessionService.updateLastActivity(sessionId);
                }

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && StringUtils.startsWithIgnoreCase(bearerToken,"Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
