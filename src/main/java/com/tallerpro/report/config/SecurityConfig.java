package com.tallerpro.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.List;

/**
 * ms-tallerpro-report es de solo lectura: KPIs para el rol Admin (panel de la red) y
 * el Jefe de taller (su propio taller).
 *
 * Capa 3 del control de acceso (ARQUITECTURA_ACCESO.md, seccion 6): el microservicio
 * vuelve a validar el JWT propagado por el gateway (firma, issuer, audience, exp) y
 * autoriza por rol. Rol insuficiente -> 403.
 *
 * TALLERPRO_JWT_ENABLED=false (solo desarrollo local sin tenant) inyecta un usuario
 * ficticio con los roles de TALLERPRO_DEV_ROLES.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    @ConditionalOnProperty(prefix = "tallerpro.security", name = "jwt-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/api/report/**").hasAnyRole("Admin", "JefeTaller")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tallerpro.security", name = "jwt-enabled", havingValue = "false")
    public SecurityFilterChain openFilterChain(HttpSecurity http,
                                               @Value("${tallerpro.security.dev-roles:Admin}") List<String> devRoles)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new DevAuthenticationFilter(devRoles), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/report/**").hasAnyRole("Admin", "JefeTaller")
                        .anyRequest().permitAll());
        return http.build();
    }

    /** App roles de Azure AD (claim "roles") -> ROLE_Admin, ROLE_Auditor, ... */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
