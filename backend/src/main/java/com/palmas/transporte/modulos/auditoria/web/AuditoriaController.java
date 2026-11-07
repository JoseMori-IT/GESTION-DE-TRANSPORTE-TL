package com.palmas.transporte.modulos.auditoria.web;


import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.auditoria.api.AuditoriaResponse;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.auditoria.application.AuditoriaConsultaService;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaConsultaService auditoriaService;

    public AuditoriaController(
            AuditoriaConsultaService auditoriaService) {

        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public Page<AuditoriaResponse> consultar(
            @RequestParam(required = false)
            String texto,

            @RequestParam(required = false)
            String usuarioCorreo,

            @RequestParam(required = false)
            String accion,

            @RequestParam(required = false)
            String modulo,

            @RequestParam(required = false)
            ResultadoAuditoria resultado,

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaDesde,

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaHasta,

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "25")
            int tamanio) {

        return auditoriaService.consultar(
            texto,
            usuarioCorreo,
            accion,
            modulo,
            resultado,
            fechaDesde,
            fechaHasta,
            pagina,
            tamanio
        );
    }
}