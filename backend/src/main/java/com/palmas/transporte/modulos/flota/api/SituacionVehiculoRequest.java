package com.palmas.transporte.modulos.flota.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record SituacionVehiculoRequest(
 @NotBlank @Pattern(regexp="BAJA") String accion,
 @NotNull @PastOrPresent LocalDate fechaEfectiva,
 @NotBlank @Size(max=1000) String motivo,
 @Size(max=2000) String observaciones,
 @Size(max=500) String referenciaSustento,
 boolean confirmarBaja){}
