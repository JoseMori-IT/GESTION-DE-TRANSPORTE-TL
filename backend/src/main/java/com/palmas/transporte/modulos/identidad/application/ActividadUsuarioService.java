package com.palmas.transporte.modulos.identidad.application;


import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ActividadUsuarioService {

    private static final int MINUTOS_ACTUALIZACION = 5;

    private final UsuarioRepository usuarioRepository;

    public ActividadUsuarioService(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarActividad(
            Long usuarioId,
            HttpServletRequest solicitud) {

        if (usuarioId == null) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuarioRepository.actualizarActividadSiCorresponde(
            usuarioId,
            ahora,
            ahora.minusMinutes(MINUTOS_ACTUALIZACION),
            obtenerDireccionIp(solicitud),
            obtenerUserAgent(solicitud)
        );
    }

    private String obtenerDireccionIp(
            HttpServletRequest solicitud) {

        if (solicitud == null) {
            return null;
        }

        String reenviada =
            solicitud.getHeader("X-Forwarded-For");

        if (
            reenviada != null
            && !reenviada.isBlank()
        ) {
            return limitar(
                reenviada.split(",")[0].trim(),
                45
            );
        }

        return limitar(
            solicitud.getRemoteAddr(),
            45
        );
    }

    private String obtenerUserAgent(
            HttpServletRequest solicitud) {

        if (solicitud == null) {
            return null;
        }

        return limitar(
            solicitud.getHeader("User-Agent"),
            500
        );
    }

    private String limitar(
            String valor,
            int longitud) {

        if (valor == null) {
            return null;
        }

        return valor.length() <= longitud
            ? valor
            : valor.substring(0, longitud);
    }
}
