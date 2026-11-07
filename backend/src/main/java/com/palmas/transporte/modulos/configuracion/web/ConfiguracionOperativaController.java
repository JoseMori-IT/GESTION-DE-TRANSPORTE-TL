package com.palmas.transporte.modulos.configuracion.web;
import com.palmas.transporte.modulos.configuracion.api.BaseOperativa;
import com.palmas.transporte.modulos.configuracion.api.ConfiguracionOperativaRequest;
import com.palmas.transporte.modulos.configuracion.api.ConfiguracionOperativaResponse;
import com.palmas.transporte.modulos.configuracion.application.ConfiguracionOperativaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/configuracion-operativa")
public class ConfiguracionOperativaController {
 private final ConfiguracionOperativaService datos;
 public ConfiguracionOperativaController(ConfiguracionOperativaService datos){this.datos=datos;}
 @GetMapping() public ConfiguracionOperativaResponse consultar(){return datos.consultar();}
 @PutMapping() public ConfiguracionOperativaResponse guardar(@Valid @RequestBody ConfiguracionOperativaRequest r,@AuthenticationPrincipal Jwt jwt){return datos.guardar(r,jwt.getSubject());}
}
