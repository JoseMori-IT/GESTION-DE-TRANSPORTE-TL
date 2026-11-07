package com.palmas.transporte.modulos.identidad.api;


import com.palmas.transporte.modulos.identidad.domain.Rol;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

    @NotBlank(
        message = "El nombre es obligatorio"
    )
    @Size(
        max = 120,
        message =
            "El nombre admite hasta 120 caracteres"
    )
    String nombres,

    /*
     * Para las cuentas CHOFER el correo
     * puede quedar vacío.
     */
    @Email(
        message =
            "El correo electrónico no es válido"
    )
    @Size(
        max = 150,
        message =
            "El correo admite hasta 150 caracteres"
    )
    String correo,

    @NotBlank(
        message = "La contraseña es obligatoria"
    )
    @Size(
        min = 8,
        max = 100,
        message =
            "La contraseña debe tener entre "
                + "8 y 100 caracteres"
    )
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY) String contrasena,

    @NotNull(
        message = "El rol es obligatorio"
    )
    Rol rol,

    Long conductorId

) {
    @Override public String toString() { return "UsuarioRequest[datos protegidos]"; }


    /*
     * Los choferes se identifican mediante el
     * DNI del conductor seleccionado.
     *
     * El resto de usuarios necesita un correo
     * corporativo de Grupo Palmas.
     */
    @AssertTrue(
        message =
            "El personal administrativo requiere "
                + "un correo @palmas.com.pe"
    )
    public boolean isIdentificadorValido() {
        if (rol == null) {
            return true;
        }

        if (rol == Rol.CHOFER) {
            return conductorId != null;
        }

        return correo != null
            && correo
                .trim()
                .toLowerCase()
                .matches(
                    "^[A-Za-z0-9._%+-]+"
                        + "@palmas\\.com\\.pe$"
                );
    }
}