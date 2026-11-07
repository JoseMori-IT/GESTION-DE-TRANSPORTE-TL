package com.palmas.transporte.modulos.identidad.infrastructure;



import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain;

class CambioContrasenaFilterTest {
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    private CambioContrasenaFilter preparar() {
        var usuarios = mock(UsuarioRepository.class);
        Usuario u = new Usuario();
        u.setCambioContrasenaRequerido(true);
        when(usuarios.findById(6L)).thenReturn(Optional.of(u));
        var jwt = Jwt.withTokenValue("token").header("alg", "HS256").claim("usuarioId", 6L).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CHOFER"))));
        return new CambioContrasenaFilter(usuarios);
    }

    @Test
    void bloqueaModuloAunqueElClienteOmitaElGuard() throws Exception {
        var filtro = preparar();
        var request = new MockHttpServletRequest("GET", "/api/chofer/servicios");
        request.setServletPath("/api/chofer/servicios");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filtro.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("CAMBIO_CONTRASENA_REQUERIDO"));
        verifyNoInteractions(chain);
    }

    @Test
    void permiteCambioDeContrasenaConSesionRestringida() throws Exception {
        var filtro = preparar();
        var request = new MockHttpServletRequest("POST", "/api/auth/cambiar-contrasena");
        request.setServletPath("/api/auth/cambiar-contrasena");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
