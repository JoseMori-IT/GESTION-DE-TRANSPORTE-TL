package com.palmas.transporte.modulos.flota.web;

import com.palmas.transporte.modulos.flota.api.GestionVehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.application.GestionVehiculoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController @RequestMapping("/api/vehiculos")
public class GestionVehiculoController {
 private final GestionVehiculoService servicio;
 public GestionVehiculoController(GestionVehiculoService servicio){this.servicio=servicio;}
 @PostMapping("/{id}/gestion") public Vehiculo guardar(@PathVariable Long id,@Valid @RequestBody GestionVehiculoRequest request,@AuthenticationPrincipal Jwt jwt){return servicio.guardar(id,request,jwt.getSubject());}
}
