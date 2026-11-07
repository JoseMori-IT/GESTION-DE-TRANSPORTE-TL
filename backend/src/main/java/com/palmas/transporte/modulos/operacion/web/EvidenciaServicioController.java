package com.palmas.transporte.modulos.operacion.web;


import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.palmas.transporte.modulos.operacion.api.EvidenciaServicioResponse;
import com.palmas.transporte.modulos.operacion.domain.EvidenciaServicio;
import com.palmas.transporte.modulos.operacion.domain.TipoEvidencia;
import com.palmas.transporte.modulos.operacion.application.EvidenciaServicioService;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciaServicioController {

    private final EvidenciaServicioService
        evidenciaService;

    public EvidenciaServicioController(
            EvidenciaServicioService
                evidenciaService) {

        this.evidenciaService =
            evidenciaService;
    }

    @GetMapping("/servicio/{servicioId}")
    public List<EvidenciaServicioResponse>
        listarPorServicio(
            @PathVariable Long servicioId,
            @AuthenticationPrincipal Jwt jwt) {

        return evidenciaService
            .listarPorServicio(
                servicioId,
                obtenerRol(jwt),
                obtenerConductorId(jwt)
            );
    }

    @PostMapping(
        value = "/servicio/{servicioId}",
        consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EvidenciaServicioResponse registrar(
            @PathVariable Long servicioId,
            @RequestParam TipoEvidencia tipo,
            @RequestParam MultipartFile archivo,
            @RequestParam(required = false)
            String descripcion,
            @RequestParam(required = false)
            BigDecimal latitud,
            @RequestParam(required = false)
            BigDecimal longitud,
            @RequestParam(required = false)
            String nombreResponsable,
            @RequestParam(required = false)
            String documentoResponsable,
            @RequestParam(required = false) String eventoUuid,
            @AuthenticationPrincipal Jwt jwt) {

        String registradoPor =
            jwt.getClaimAsString("nombre");

        if (
            registradoPor == null
            || registradoPor.isBlank()
        ) {
            registradoPor =
                jwt.getSubject();
        }

        return evidenciaService.registrar(
            servicioId,
            tipo,
            archivo,
            descripcion,
            latitud,
            longitud,
            nombreResponsable,
            documentoResponsable,
            registradoPor,
            obtenerRol(jwt),
            obtenerConductorId(jwt),
            eventoUuid
        );
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        EvidenciaServicio evidencia =
            evidenciaService.buscarPorId(
                id,
                obtenerRol(jwt),
                obtenerConductorId(jwt)
            );

        Resource recurso =
            evidenciaService.cargarArchivo(
                evidencia
            );

        ContentDisposition disposicion =
            ContentDisposition
                .attachment()
                .filename(
                    evidencia
                        .getNombreOriginal(),
                    StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
            /*
             * Fuerza descarga y evita ejecución
             * de contenido activo en el navegador.
             */
            .contentType(
                MediaType
                    .APPLICATION_OCTET_STREAM
            )
            .contentLength(
                evidencia.getTamanoBytes()
            )
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                disposicion.toString()
            )
            .header(
                "X-Content-Type-Options",
                "nosniff"
            )
            .header(
                "Content-Security-Policy",
                "sandbox"
            )
            .header(
                HttpHeaders.CACHE_CONTROL,
                "no-store"
            )
            .body(recurso);
    }

    private String obtenerRol(
            Jwt jwt) {

        return jwt != null
            ? jwt.getClaimAsString("rol")
            : null;
    }

    private Long obtenerConductorId(
            Jwt jwt) {

        if (jwt == null) {
            return null;
        }

        Object valor =
            jwt.getClaim("conductorId");

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        if (valor != null) {
            try {
                return Long.valueOf(
                    valor.toString()
                );
            } catch (
                NumberFormatException ignored
            ) {
                return null;
            }
        }

        return null;
    }
}
