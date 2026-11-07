package com.palmas.transporte.modulos.operacion.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record IncidenciaViajeRequest(
 @NotBlank @Size(max=40) String accion,
 @Size(max=1000) String descripcion,
 @Size(max=150) String lugar,
 @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
 @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
 @Size(max=150) String tecnico,
 @Size(max=1500) String diagnostico,
 Long vehiculoAuxilioId, Long conductorAuxilioId,
 @DecimalMin("0") @DecimalMax("99999999.99") BigDecimal odometroRegresoKm,
 Long incidenciaId, Long version
){}
