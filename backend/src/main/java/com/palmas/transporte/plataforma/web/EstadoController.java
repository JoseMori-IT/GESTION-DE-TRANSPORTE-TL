package com.palmas.transporte.plataforma.web;


import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EstadoController {

    @GetMapping("/estado")
    public Map<String, Object> obtenerEstado() {
        return Map.of(
            "aplicacion", "Sistema de Gestión de Transporte",
            "estado", "Operativo",
            "version", "Beta 1.0",
            "fecha", LocalDateTime.now()
        );
    }
}