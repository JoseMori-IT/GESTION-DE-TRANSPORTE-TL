package com.palmas.transporte.modulos.abastecimientos.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record AbastecimientoRequest(@NotNull Long vehiculoId,@NotNull LocalDateTime fecha,
 @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("10000") @Digits(integer=7,fraction=3) BigDecimal galones,
 @DecimalMin("0") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) BigDecimal odometroKm,
 @Size(max=300) String motivoSinOdometro,@NotBlank @Size(max=150) String grifo,
 @Size(max=100) String comprobante,@Size(max=500) String observaciones,@NotBlank @Size(max=36) String eventoUuid){}
