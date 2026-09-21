package com.tallerpro.report.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Evidencia 401 / 403 / 200 (ARQUITECTURA_ACCESO.md, seccion 9) con la validacion JWT activa. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /** Sin tenant en los tests: evita que Spring descargue el JWKS. */
    @MockBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor conRol(String... roles) {
        return jwt()
                .jwt(j -> j.claim("oid", "user").claim("roles", List.of(roles)))
                .authorities(Arrays.stream(roles)
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                        .toList());
    }

    @Test
    void sinTokenRespondeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/report/kpis/active-orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void rolInsuficienteRespondeForbidden() throws Exception {
        mockMvc.perform(get("/api/report/kpis/active-orders").with(conRol("Cliente"))).andExpect(status().isForbidden());
    }

    @Test
    void tokenSinRolesRespondeForbidden() throws Exception {
        mockMvc.perform(get("/api/report/kpis/active-orders").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void rolCorrectoRespondeOk() throws Exception {
        for (String rol : new String[]{"Admin", "JefeTaller"}) {
            mockMvc.perform(get("/api/report/kpis/active-orders").with(conRol(rol))).andExpect(status().isOk());
        }
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
