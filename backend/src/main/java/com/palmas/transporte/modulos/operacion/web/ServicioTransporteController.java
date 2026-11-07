package com.palmas.transporte.modulos.operacion.web;


import com.palmas.transporte.modulos.operacion.api.ServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.application.ServicioTransporteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.palmas.transporte.modulos.operacion.api.EstadoServicioRequest;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servicios")
public class ServicioTransporteController {

    private final ServicioTransporteService servicioService;

    public ServicioTransporteController(
        ServicioTransporteService servicioService
    ) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public List<ServicioTransporte> listarTodos() {
        return servicioService.listarTodos();
    }

    @GetMapping("/{id}")
    public ServicioTransporte buscarPorId(
        @PathVariable Long id
    ) {
        return servicioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioTransporte registrar(
        @Valid @RequestBody ServicioRequest request
    ) {
        return servicioService.registrar(request);
    }

    @PatchMapping("/{id}/estado")
public ServicioTransporte cambiarEstado(
    @PathVariable Long id,
    @Valid @RequestBody EstadoServicioRequest request
) {
    return servicioService.cambiarEstado(
        id,
        request.estado()
    );
}


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarError(
        IllegalArgumentException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(Map.of("mensaje", exception.getMessage()));
    }
}