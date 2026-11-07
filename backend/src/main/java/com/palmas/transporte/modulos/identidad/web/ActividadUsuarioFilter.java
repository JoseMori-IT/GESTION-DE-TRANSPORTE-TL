package com.palmas.transporte.modulos.identidad.web;


import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.palmas.transporte.modulos.identidad.application.ActividadUsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ActividadUsuarioFilter
        extends OncePerRequestFilter {

    private final ActividadUsuarioService actividadService;

    public ActividadUsuarioFilter(
            ActividadUsuarioService actividadService) {

        this.actividadService = actividadService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication autenticacion =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (
            autenticacion != null
            && autenticacion.isAuthenticated()
            && autenticacion.getPrincipal()
                instanceof Jwt jwt
        ) {
            actividadService.registrarActividad(
                obtenerUsuarioId(jwt),
                request
            );
        }

        filterChain.doFilter(
            request,
            response
        );
    }

    private Long obtenerUsuarioId(Jwt jwt) {
        Object valor = jwt.getClaim("usuarioId");

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        if (valor != null) {
            try {
                return Long.valueOf(
                    valor.toString()
                );
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
