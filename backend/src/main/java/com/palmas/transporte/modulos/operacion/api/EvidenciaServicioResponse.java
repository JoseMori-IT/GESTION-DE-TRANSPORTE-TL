package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.TipoEvidencia;

public record EvidenciaServicioResponse(
    Long id,
    Long servicioId,
    String servicioCodigo,
    TipoEvidencia tipo,
    String nombreOriginal,
    String tipoContenido,
    Long tamanoBytes,
    String descripcion,
    BigDecimal latitud,
    BigDecimal longitud,
    String nombreResponsable,
    String documentoResponsable,
    String registradoPor,
    LocalDateTime fechaRegistro,
    String urlArchivo
) {
}