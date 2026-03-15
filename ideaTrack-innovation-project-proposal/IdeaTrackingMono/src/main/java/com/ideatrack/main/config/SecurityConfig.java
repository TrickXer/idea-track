package com.ideatrack.main.config;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Needed for @PreAuthorize to work
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        try {
            return config.getAuthenticationManager();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure authentication manager", e);
        }
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Stateless JWT API => CSRF not needed
                .csrf(csrf -> csrf.disable())

                // If you want CORS, configure it properly instead of disabling:
                // .cors(Customizer.withDefaults())
                //.cors(cors -> cors.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Async dispatches (e.g. SSE SseEmitter) must be permitted — they are
                        // continuations of authenticated requests and carry no Authorization header.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/profile/departments").permitAll()
                        .requestMatchers("/uploads/profile-pics/**").permitAll()

                        // Admin endpoints accessible by ADMIN and SUPERADMIN
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "SUPERADMIN" )
                        .requestMatchers("/uploads/profile-pics/**").permitAll()
                        //.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(
                                    "{\"error\":\"Forbidden\",\"message\":\"Access denied\"}"
                            );
                        })
                );

        //  IMPORTANT: JWT filter must run before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}