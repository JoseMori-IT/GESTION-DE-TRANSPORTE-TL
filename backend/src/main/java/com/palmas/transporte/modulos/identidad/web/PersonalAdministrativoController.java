package com.palmas.transporte.modulos.identidad.web;


import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.identidad.api.DesactivarPersonalRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoActualizarRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoResponse;
import com.palmas.transporte.modulos.identidad.api.ReactivarPersonalRequest;
import com.palmas.transporte.modulos.identidad.application.PersonalAdministrativoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/personal-administrativo")
public class PersonalAdministrativoController {

    private final PersonalAdministrativoService
        personalService;

    public PersonalAdministrativoController(
            PersonalAdministrativoService
                personalService) {

        this.personalService = personalService;
    }

    @GetMapping
    public ResponseEntity<
        List<PersonalAdministrativoResponse>
    > listar(@AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
            personalService.listar(jwt)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<
        PersonalAdministrativoResponse
    > buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
            personalService.buscarPorId(id, jwt)
        );
    }

    @PostMapping
    public ResponseEntity<
        PersonalAdministrativoResponse
    > registrar(
            @Valid
            @RequestBody
            PersonalAdministrativoRequest request,

            @AuthenticationPrincipal
            Jwt jwt,

            HttpServletRequest solicitudHttp) {

        PersonalAdministrativoResponse personal =
            personalService.registrar(
                request,
                jwt,
                solicitudHttp
            );

        return ResponseEntity
            .created(
                URI.create(
                    "/api/personal-administrativo/"
                    + personal.id()
                )
            )
            .body(personal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<
        PersonalAdministrativoResponse
    > actualizar(
            @PathVariable Long id,

            @Valid
            @RequestBody
            PersonalAdministrativoActualizarRequest
                request,

            @AuthenticationPrincipal
            Jwt jwt,

            HttpServletRequest solicitudHttp) {

        return ResponseEntity.ok(
            personalService.actualizar(
                id,
                request,
                jwt,
                solicitudHttp
            )
        );
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<
        PersonalAdministrativoResponse
    > desactivar(
            @PathVariable Long id,

            @Valid
            @RequestBody
            DesactivarPersonalRequest request,

            @AuthenticationPrincipal
            Jwt jwt,

            HttpServletRequest solicitudHttp) {

        return ResponseEntity.ok(
            personalService.desactivar(
                id,
                request,
                obtenerUsuarioId(jwt),
                jwt,
                solicitudHttp
            )
        );
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<
        PersonalAdministrativoResponse
    > reactivar(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ReactivarPersonalRequest request,

            @AuthenticationPrincipal
            Jwt jwt,

            HttpServletRequest solicitudHttp) {

        return ResponseEntity.ok(
            personalService.reactivar(
                id,
                request,
                jwt,
                solicitudHttp
            )
        );
    }

    private Long obtenerUsuarioId(Jwt jwt) {
        Object claim = jwt.getClaim("usuarioId");

        if (claim instanceof Number numero) {
            return numero.longValue();
        }

        try {
            return Long.valueOf(
                claim.toString()
            );
        } catch (
            NullPointerException
            | NumberFormatException error
        ) {
            return null;
        }
    }
}
