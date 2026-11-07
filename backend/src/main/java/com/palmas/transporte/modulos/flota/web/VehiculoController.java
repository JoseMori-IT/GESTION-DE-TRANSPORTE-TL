package com.palmas.transporte.modulos.flota.web;
import com.palmas.transporte.modulos.flota.application.OdometroService;
import com.palmas.transporte.modulos.flota.domain.MovimientoOdometro;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

import com.palmas.transporte.modulos.flota.api.VehiculoEstadoRequest;
import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.application.VehiculoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final com.palmas.transporte.modulos.flota.application.OdometroService odometros;

    public VehiculoController(
            VehiculoService vehiculoService, com.palmas.transporte.modulos.flota.application.OdometroService odometros) {

        this.vehiculoService = vehiculoService; this.odometros=odometros;
    }

    @GetMapping
    public List<Vehiculo> listar() {
        return vehiculoService.listar();
    }

    @GetMapping("/{id}/odometro/historial")
    public java.util.List<com.palmas.transporte.modulos.flota.domain.MovimientoOdometro> historialOdometro(@PathVariable Long id){return odometros.historial(id);}

    @GetMapping("/{id}")
    public Vehiculo buscarPorId(
            @PathVariable Long id) {

        return vehiculoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Vehiculo> registrar(
            @Valid @RequestBody VehiculoRequest request) {

        Vehiculo vehiculo =
            vehiculoService.registrar(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/vehiculos/" + vehiculo.getId()
                )
            )
            .body(vehiculo);
    }

    @PutMapping("/{id}")
    public Vehiculo actualizar(
            @PathVariable Long id,
            @Valid @RequestBody VehiculoRequest request) {

        return vehiculoService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public Vehiculo cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody
            VehiculoEstadoRequest request) {

        return vehiculoService.cambiarEstadoManual(
            id,
            request.estado()
        );
    }

    @PatchMapping("/{id}/desactivar")
    public Vehiculo desactivar(
            @PathVariable Long id) {

        return vehiculoService.desactivar(id);
    }

    @PostMapping(
        value = "/{id}/foto",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Vehiculo guardarFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        return vehiculoService.guardarFoto(id, archivo);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> obtenerFoto(
            @PathVariable Long id) {

        VehiculoService.FotoDescargable foto =
            vehiculoService.obtenerFoto(id);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                foto.tipoContenido()
            ))
            .cacheControl(CacheControl.noStore())
            .header(
                "Content-Security-Policy",
                "sandbox"
            )
            .header(
                "X-Content-Type-Options",
                "nosniff"
            )
            .body(foto.contenido());
    }

    @DeleteMapping("/{id}/foto")
    public Vehiculo eliminarFoto(
            @PathVariable Long id) {

        return vehiculoService.eliminarFoto(id);
    }
}
