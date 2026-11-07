package com.palmas.transporte.modulos.evaluacion.api;

import jakarta.validation.constraints.*;
public record ValidacionViajeRequest(@NotNull Long servicioId,
 @NotBlank @Pattern(regexp="VALIDADO|OBSERVADO") String estado,
 @Size(max=1000) String comentario, Long ultimaValidacionId) {}
