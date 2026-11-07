package com.palmas.transporte.modulos.evaluacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.evaluacion.domain.TipoEvaluador;

public record EncuestaServicioResponse(
    Long id,
    Long servicioId,
    String servicioCodigo,
    Integer puntualidad,
    Integer cumplimiento,
    Integer atencion,
    Integer satisfaccionGeneral,
    BigDecimal promedio,
    String comentarios,
    String evaluadorNombre,
    String evaluadorCorreo,
    TipoEvaluador tipoEvaluador,
    LocalDateTime fechaRegistro
) {
}