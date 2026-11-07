package com.palmas.transporte.plataforma.web;


import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Evita almacenar respuestas API con datos personales o credenciales temporales. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrivacidadFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String ruta = request.getServletPath();
        if (ruta == null || ruta.isEmpty()) ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (ruta.equals("/api") || ruta.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        chain.doFilter(request, response);
    }
}
