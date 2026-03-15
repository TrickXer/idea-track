package com.ideatrack.main.config;



import com.ideatrack.main.service.MyUserDetailsService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String QUOTE = "\"";

    private final MyUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public JwtFilter(MyUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip JWT processing for async dispatches (e.g. SSE SseEmitter callbacks).
        // Async dispatches do not carry the Authorization header and are continuations
        // of an already-authenticated request — re-filtering them causes Access Denied.
        return request.getDispatcherType() == DispatcherType.ASYNC;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        String username = null;
        String jwt = null;

        try {
            if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(BEARER_PREFIX)) {
                // safer extraction than indexOf(' ')
                jwt = authorizationHeader.substring(BEARER_PREFIX_LENGTH).trim();

                // If token was copied with quotes, remove them
                if (jwt.length() >= 2 && jwt.startsWith(QUOTE) && jwt.endsWith(QUOTE)) {
                    jwt = jwt.substring(1, jwt.length() - 1).trim();
                }

                username = jwtUtil.extractUsername(jwt);
            }
        } catch (Exception e) {
            logger.error("JWT Token extraction failed: " + e.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Load user to validate token subject/expiration against known user
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // ✅ NEW: build authorities from JWT roles claim
                    List<SimpleGrantedAuthority> authoritiesFromToken =
                            jwtUtil.extractRoles(jwt).stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList();

                    // Fallback: if token doesn't contain roles for some reason, use DB authorities
                    var finalAuthorities = authoritiesFromToken.isEmpty()
                            ? userDetails.getAuthorities()
                            : authoritiesFromToken;

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, finalAuthorities);

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Optional debug (keep for troubleshooting)
                    logger.info("Authenticated: " + username + " authorities=" + finalAuthorities);
                }
            } catch (Exception e) {
                logger.error("JWT validation failed: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}