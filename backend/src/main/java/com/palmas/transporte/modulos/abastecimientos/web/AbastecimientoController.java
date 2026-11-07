package com.palmas.transporte.modulos.abastecimientos.web;

import com.palmas.transporte.modulos.abastecimientos.domain.Abastecimiento;
import com.palmas.transporte.modulos.abastecimientos.api.AbastecimientoRequest;
import com.palmas.transporte.modulos.abastecimientos.application.AbastecimientoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
@RestController @RequestMapping("/api/abastecimientos")
public class AbastecimientoController{
 private final AbastecimientoService servicio;public AbastecimientoController(AbastecimientoService s){servicio=s;}
 @GetMapping public List<Abastecimiento> listar(){return servicio.listar();}
 @PostMapping public Abastecimiento registrar(@Valid @RequestBody AbastecimientoRequest r,@AuthenticationPrincipal Jwt jwt){return servicio.registrar(r,jwt.getSubject());}
 public record Anular(@NotNull Long version,@NotBlank @Size(min=5,max=300) String motivo){}
 @PostMapping("/{id}/anular") public Abastecimiento anular(@PathVariable Long id,@Valid @RequestBody Anular r,@AuthenticationPrincipal Jwt jwt){return servicio.anular(id,r.version(),r.motivo(),jwt.getSubject());}
}
