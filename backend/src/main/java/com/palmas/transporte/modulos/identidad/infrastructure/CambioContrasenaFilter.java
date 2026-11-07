package com.palmas.transporte.modulos.identidad.infrastructure;


import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CambioContrasenaFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    public CambioContrasenaFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        String ruta = request.getServletPath();
        boolean permitido = "OPTIONS".equals(request.getMethod())
            || ("POST".equals(request.getMethod()) && java.util.Set.of(
                "/api/auth/login", "/api/auth/logout", "/api/auth/cambiar-contrasena").contains(ruta))
            || ("GET".equals(request.getMethod()) && java.util.Set.of(
                "/api/perfil", "/api/estado").contains(ruta));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!permitido && auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
            Object id = jwt.getClaim("usuarioId");
            if (id instanceof Number numero && usuarios.findById(numero.longValue())
                .map(u -> Boolean.TRUE.equals(u.getCambioContrasenaRequerido())).orElse(false)) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"estado\":403,\"codigo\":\"CAMBIO_CONTRASENA_REQUERIDO\","
                    + "\"mensaje\":\"Debes cambiar tu contrasena antes de continuar\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
