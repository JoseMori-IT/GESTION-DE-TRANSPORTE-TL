package com.palmas.transporte.modulos.flota.web;

import com.palmas.transporte.modulos.flota.api.SituacionVehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.MovimientoVehiculo;
import com.palmas.transporte.modulos.flota.application.SituacionVehiculoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping("/api/vehiculos")
public class SituacionVehiculoController {
 private final SituacionVehiculoService servicio;
 public SituacionVehiculoController(SituacionVehiculoService servicio){this.servicio=servicio;}
 @GetMapping("/{id}/situacion") public List<MovimientoVehiculo> historial(@PathVariable Long id){return servicio.historial(id);}
 @PostMapping("/{id}/situacion") public MovimientoVehiculo registrar(@PathVariable Long id,@Valid @RequestBody SituacionVehiculoRequest r,@AuthenticationPrincipal Jwt jwt){return servicio.registrar(id,r,jwt.getClaimAsString("rol"),jwt.getSubject(),jwt.getClaimAsString("nombre"));}
}
