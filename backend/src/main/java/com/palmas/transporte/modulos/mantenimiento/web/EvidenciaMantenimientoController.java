package com.palmas.transporte.modulos.mantenimiento.web;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;

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

import com.palmas.transporte.modulos.mantenimiento.api.EvidenciaMantenimientoResponse;
import com.palmas.transporte.modulos.mantenimiento.domain.EvidenciaMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.application.EvidenciaMantenimientoService;

@RestController
@RequestMapping("/api/mantenimientos")
public class EvidenciaMantenimientoController {

    private final EvidenciaMantenimientoService
        evidenciaService;

    public EvidenciaMantenimientoController(
        EvidenciaMantenimientoService evidenciaService
    ) {
        this.evidenciaService =
            evidenciaService;
    }

    @GetMapping("/{mantenimientoId}/evidencias")
    public List<EvidenciaMantenimientoResponse>
        listar(
            @PathVariable Long mantenimientoId,
            @AuthenticationPrincipal Jwt jwt
        ) {

        return evidenciaService
            .listarPorMantenimiento(
                mantenimientoId,
                obtenerRol(jwt)
            );
    }

    @PostMapping(
        value = "/{mantenimientoId}/evidencias",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EvidenciaMantenimientoResponse registrar(
        @PathVariable Long mantenimientoId,

        @RequestParam
        TipoEvidenciaMantenimiento tipo,

        @RequestParam
        MultipartFile archivo,

        @RequestParam(required = false)
        String descripcion,

        @AuthenticationPrincipal Jwt jwt
    ) {
        return evidenciaService.registrar(
            mantenimientoId,
            tipo,
            archivo,
            descripcion,
            obtenerNombreUsuario(jwt),
            obtenerRol(jwt)
        );
    }

    @GetMapping("/evidencias/{id}/archivo")
    public ResponseEntity<Resource> archivo(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String rol =
            obtenerRol(jwt);

        EvidenciaMantenimiento evidencia =
            evidenciaService.buscarPorId(
                id,
                rol
            );

        Resource recurso =
            evidenciaService.cargarArchivo(
                evidencia
            );

        ContentDisposition disposicion =
            ContentDisposition.attachment()
                .filename(
                    evidencia.getNombreOriginal(),
                    StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
            .contentType(
                MediaType.APPLICATION_OCTET_STREAM
            )
            .contentLength(
                evidencia.getTamanioBytes()
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
            .header(
                HttpHeaders.PRAGMA,
                "no-cache"
            )
            .header(
                HttpHeaders.EXPIRES,
                "0"
            )
            .body(recurso);
    }

    private String obtenerNombreUsuario(
        Jwt jwt
    ) {
        if (jwt == null) {
            return "Usuario del sistema";
        }

        String nombre =
            jwt.getClaimAsString("nombre");

        if (
            nombre != null
            && !nombre.isBlank()
        ) {
            return nombre.trim();
        }

        String subject =
            jwt.getSubject();

        if (
            subject != null
            && !subject.isBlank()
        ) {
            return subject.trim();
        }

        return "Usuario del sistema";
    }

    private String obtenerRol(
        Jwt jwt
    ) {
        if (jwt == null) {
            return null;
        }

        return jwt.getClaimAsString("rol");
    }
}

