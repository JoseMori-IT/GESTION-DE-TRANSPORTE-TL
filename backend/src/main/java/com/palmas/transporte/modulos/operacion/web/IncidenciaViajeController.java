package com.palmas.transporte.modulos.operacion.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.palmas.transporte.modulos.operacion.api.IncidenciaViajeRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.application.IncidenciaViajeService;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/incidencias-viaje")
public class IncidenciaViajeController {
 private final IncidenciaViajeService service;
 public IncidenciaViajeController(IncidenciaViajeService service){this.service=service;}
 @PostMapping("/{servicioId}") public ServicioTransporte gestionar(@PathVariable Long servicioId,@Valid @RequestBody IncidenciaViajeRequest r,@AuthenticationPrincipal Jwt jwt){return service.gestionar(servicioId,r,jwt.getClaimAsString("rol"),jwt.getSubject(),jwt.getClaim("conductorId"));}
}
