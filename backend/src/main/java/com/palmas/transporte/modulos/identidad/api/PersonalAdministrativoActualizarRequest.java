package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;

import com.palmas.transporte.modulos.identidad.domain.CargoAdministrativo;
import com.palmas.transporte.modulos.identidad.domain.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PersonalAdministrativoActualizarRequest(

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
        regexp = "\\d{8}",
        message = "El DNI debe contener 8 dígitos"
    )
    String dni,

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 120)
    String nombres,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 120)
    String apellidos,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Size(max = 150)
    String correo,

    @NotNull(message = "El cargo es obligatorio")
    CargoAdministrativo cargo,

    @NotNull(message = "El perfil de acceso es obligatorio")
    Rol rol,

    @NotNull(message = "La fecha de ingreso es obligatoria")
    LocalDate fechaIngreso

) {
}
