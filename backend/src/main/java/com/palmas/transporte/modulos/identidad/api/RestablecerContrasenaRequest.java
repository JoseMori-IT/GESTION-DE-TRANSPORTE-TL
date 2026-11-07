package com.palmas.transporte.modulos.identidad.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaRequest(
    @NotBlank @Size(max = 500) String motivo
) {}
