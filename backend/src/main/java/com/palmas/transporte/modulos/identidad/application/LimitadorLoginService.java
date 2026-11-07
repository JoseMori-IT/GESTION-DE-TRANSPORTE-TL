package com.palmas.transporte.modulos.identidad.application;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LimitadorLoginService {

    private static final long
        FRECUENCIA_LIMPIEZA = 256L;

    private final Map<String, VentanaIntentos>
        ventanas = new ConcurrentHashMap<>();

    private final AtomicLong solicitudesProcesadas =
        new AtomicLong();

    private final int maximoIntentos;
    private final long duracionVentanaSegundos;

    public LimitadorLoginService(
        @Value(
            "${app.auth.limite-ip-intentos:120}"
        )
        int maximoIntentos,

        @Value(
            "${app.auth.limite-ip-ventana-segundos:300}"
        )
        long duracionVentanaSegundos
    ) {
        if (
            maximoIntentos < 20
            || maximoIntentos > 1000
        ) {
            throw new IllegalStateException(
                "app.auth.limite-ip-intentos debe "
                    + "estar entre 20 y 1000"
            );
        }

        if (
            duracionVentanaSegundos < 60
            || duracionVentanaSegundos > 3600
        ) {
            throw new IllegalStateException(
                "app.auth.limite-ip-ventana-segundos "
                    + "debe estar entre 60 y 3600"
            );
        }

        this.maximoIntentos =
            maximoIntentos;

        this.duracionVentanaSegundos =
            duracionVentanaSegundos;
    }

    public void verificar(
        HttpServletRequest solicitud
    ) {
        String direccionIp =
            obtenerDireccionIp(solicitud);

        long ahora =
            Instant.now()
                .getEpochSecond();

        Resultado resultado =
            new Resultado();

        ventanas.compute(
            direccionIp,
            (ip, ventanaActual) -> {
                if (
                    ventanaActual == null
                    || ventanaVencida(
                        ventanaActual,
                        ahora
                    )
                ) {
                    resultado.permitido = true;

                    return new VentanaIntentos(
                        ahora,
                        1
                    );
                }

                int nuevosIntentos =
                    ventanaActual.intentos() + 1;

                resultado.permitido =
                    nuevosIntentos
                        <= maximoIntentos;

                return new VentanaIntentos(
                    ventanaActual.inicioEpoch(),
                    nuevosIntentos
                );
            }
        );

        limpiarPeriodicamente(ahora);

        if (!resultado.permitido) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas solicitudes de acceso. "
                    + "Intenta nuevamente más tarde"
            );
        }
    }

    private boolean ventanaVencida(
        VentanaIntentos ventana,
        long ahora
    ) {
        return ahora
            - ventana.inicioEpoch()
            >= duracionVentanaSegundos;
    }

    private void limpiarPeriodicamente(
        long ahora
    ) {
        long total =
            solicitudesProcesadas
                .incrementAndGet();

        if (
            total % FRECUENCIA_LIMPIEZA
                != 0
        ) {
            return;
        }

        long antiguedadMaxima =
            duracionVentanaSegundos * 2L;

        ventanas.entrySet().removeIf(
            entrada ->
                ahora
                    - entrada
                        .getValue()
                        .inicioEpoch()
                    >= antiguedadMaxima
        );
    }

    private String obtenerDireccionIp(
        HttpServletRequest solicitud
    ) {
        if (
            solicitud == null
            || solicitud.getRemoteAddr() == null
            || solicitud
                .getRemoteAddr()
                .isBlank()
        ) {
            return "direccion-desconocida";
        }

        String direccion =
            solicitud
                .getRemoteAddr()
                .trim();

        return direccion.length() <= 45
            ? direccion
            : direccion.substring(
                0,
                45
            );
    }

    private record VentanaIntentos(
        long inicioEpoch,
        int intentos
    ) {
    }

    private static final class Resultado {

        private boolean permitido;
    }
}