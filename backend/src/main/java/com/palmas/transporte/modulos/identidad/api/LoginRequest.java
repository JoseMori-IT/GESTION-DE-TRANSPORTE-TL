package com.palmas.transporte.modulos.identidad.api;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

    @NotBlank(
        message =
            "El correo corporativo o DNI es obligatorio"
    )
    @Pattern(
        regexp =
            "^(?:[A-Za-z0-9._%+-]+@palmas\\.com\\.pe|\\d{8})$",
        message =
            "Ingresa un correo @palmas.com.pe o un DNI de 8 dígitos"
    )
    String identificador,

    @NotBlank(
        message = "La contraseña es obligatoria"
    )
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY) String contrasena

) {
    @Override public String toString() { return "LoginRequest[datos protegidos]"; }

}