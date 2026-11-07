package com.palmas.transporte.modulos.identidad.api;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;

public record CambiarContrasenaRequest(
    @NotBlank @Size(max = 100) @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY) String contrasenaActual,
    @NotBlank @Size(min = 12, max = 64,
        message = "La nueva contrasena debe tener entre 12 y 64 caracteres")
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY) String contrasenaNueva
) {
    @Override public String toString() { return "CambiarContrasenaRequest[datos protegidos]"; }

    @AssertTrue(message = "La nueva contrasena supera el limite de 72 bytes UTF-8")
    public boolean isLongitudSegura() {
        return contrasenaNueva == null
            || contrasenaNueva.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
