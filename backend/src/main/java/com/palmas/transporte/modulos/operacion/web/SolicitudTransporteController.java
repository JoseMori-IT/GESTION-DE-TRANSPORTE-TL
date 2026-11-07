package com.palmas.transporte.modulos.operacion.web;
import com.palmas.transporte.modulos.operacion.api.RecorridoSolicitudRequest;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.operacion.api.ProgramarSolicitudRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudEstadoRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudTransporteRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudTransporteResponse;
import com.palmas.transporte.modulos.operacion.application.SolicitudTransporteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudTransporteController {

    private final SolicitudTransporteService
        solicitudService;

    public SolicitudTransporteController(
        SolicitudTransporteService solicitudService
    ) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<SolicitudTransporteResponse> listar() {
        return solicitudService.listarTodas();
    }

    @GetMapping("/{id}")
    public SolicitudTransporteResponse buscarPorId(
        @PathVariable Long id
    ) {
        return solicitudService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<SolicitudTransporteResponse>
        registrar(
            @Valid
            @RequestBody
            SolicitudTransporteRequest request,

            @AuthenticationPrincipal
            Jwt jwt
        ) {

        SolicitudTransporteResponse registrada =
            solicitudService.registrar(
                request,
                jwt.getSubject()
            );

        return ResponseEntity
            .created(
                URI.create(
                    "/api/solicitudes/"
                        + registrada.id()
                )
            )
            .body(registrada);
    }

    @PatchMapping("/{id}/estado")
    public SolicitudTransporteResponse cambiarEstado(
        @PathVariable Long id,

        @Valid
        @RequestBody
        SolicitudEstadoRequest request
    ) {
        return solicitudService.cambiarEstado(
            id,
            request.estado(),
            request.motivo()
        );
    }

    @PatchMapping("/{id}/recorrido")
    public SolicitudTransporteResponse completarRecorrido(@PathVariable Long id,@Valid @RequestBody com.palmas.transporte.modulos.operacion.api.RecorridoSolicitudRequest request) {
        return solicitudService.completarRecorrido(id,request);
    }
    @PostMapping("/{id}/programar")
    public SolicitudTransporteResponse programar(
        @PathVariable Long id,

        @Valid
        @RequestBody
        ProgramarSolicitudRequest request
    ) {
        return solicitudService.programar(
            id,
            request
        );
    }
}