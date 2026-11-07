package com.palmas.transporte.modulos.configuracion.api;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ConfiguracionOperativaRequest(@PositiveOrZero Long version,
 @NotBlank @Size(max=100) String sede,
 @Positive Long plantaBaseId,
 @NotNull @DecimalMin("0.000001") @DecimalMax("100") @Digits(integer=3,fraction=6) BigDecimal galonesPorKm,
 @NotNull @DecimalMin("0") @DecimalMax("1000") @Digits(integer=4,fraction=6) BigDecimal kgCo2PorKm,
 @NotBlank @Size(max=500) String referenciaCombustible,
 @NotBlank @Size(max=500) String referenciaEmisiones) {}
