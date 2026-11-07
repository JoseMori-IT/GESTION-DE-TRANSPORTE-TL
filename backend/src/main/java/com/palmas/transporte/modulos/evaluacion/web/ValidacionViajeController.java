package com.palmas.transporte.modulos.evaluacion.web;

import com.palmas.transporte.modulos.evaluacion.domain.ValidacionViaje;
import com.palmas.transporte.modulos.evaluacion.application.ValidacionViajeService;
import com.palmas.transporte.modulos.evaluacion.api.ValidacionViajeRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping("/api/validaciones-viaje")
public class ValidacionViajeController {
 private final ValidacionViajeService servicio;
 public ValidacionViajeController(ValidacionViajeService servicio){this.servicio=servicio;}
 @GetMapping public List<ValidacionViaje> listar(){return servicio.listar();}
 @PostMapping public ValidacionViaje registrar(@Valid @RequestBody ValidacionViajeRequest request,@AuthenticationPrincipal Jwt jwt){return servicio.registrar(request,jwt.getClaimAsString("rol"),jwt.getSubject(),jwt.getClaimAsString("nombre"));}
}
