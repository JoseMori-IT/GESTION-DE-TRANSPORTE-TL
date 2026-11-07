package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;

import com.palmas.transporte.modulos.identidad.domain.CargoAdministrativo;
import com.palmas.transporte.modulos.identidad.domain.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PersonalAdministrativoRequest(

    @NotBlank(
        message = "El DNI es obligatorio"
    )
    @Pattern(
        regexp = "\\d{8}",
        message =
            "El DNI debe contener 8 dígitos"
    )
    String dni,

    @NotBlank(
        message =
            "Los nombres son obligatorios"
    )
    @Size(
        max = 120,
        message =
            "Los nombres admiten hasta "
                + "120 caracteres"
    )
    String nombres,

    @NotBlank(
        message =
            "Los apellidos son obligatorios"
    )
    @Size(
        max = 120,
        message =
            "Los apellidos admiten hasta "
                + "120 caracteres"
    )
    String apellidos,

    @NotBlank(
        message =
            "El correo corporativo es obligatorio"
    )
    @Email(
        message =
            "El correo electrónico no es válido"
    )
    @Pattern(
        regexp =
            "(?i)^[A-Z0-9._%+-]+"
                + "@palmas\\.com\\.pe$",
        message =
            "Solo se permiten correos corporativos "
                + "@palmas.com.pe"
    )
    @Size(
        max = 150,
        message =
            "El correo admite hasta 150 caracteres"
    )
    String correo,

    @NotBlank(
        message =
            "La contraseña es obligatoria"
    )
    @Size(
        min = 8,
        max = 100,
        message =
            "La contraseña debe tener entre "
                + "8 y 100 caracteres"
    )
    String contrasena,

    @NotNull(
        message = "El cargo es obligatorio"
    )
    CargoAdministrativo cargo,

    @NotNull(message = "El perfil de acceso es obligatorio")
    Rol rol,

    @NotNull(
        message =
            "La fecha de ingreso es obligatoria"
    )
    LocalDate fechaIngreso

) {
}
