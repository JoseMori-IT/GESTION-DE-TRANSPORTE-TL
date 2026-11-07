package com.palmas.transporte.modulos.identidad.web;
import com.palmas.transporte.modulos.identidad.application.RenovacionSesionService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import java.util.Map;
@RestController
public class RenovacionSesionController {
 private final RenovacionSesionService sesiones;
 public RenovacionSesionController(RenovacionSesionService sesiones){this.sesiones=sesiones;}
 @PostMapping("/api/auth/renovar")
 public ResponseEntity<Map<String,String>> renovar(@AuthenticationPrincipal Jwt jwt){return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("token",sesiones.renovar(jwt)));}
}
