package com.palmas.transporte.modulos.mantenimiento.api;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;

import java.time.LocalDateTime;



public record EvidenciaMantenimientoResponse(

    Long id,

    Long mantenimientoId,
    String mantenimientoCodigo,

    Long vehiculoId,
    String vehiculoPlaca,

    TipoEvidenciaMantenimiento tipo,

    String nombreOriginal,
    String tipoContenido,
    Long tamanioBytes,
    String descripcion,

    String registradoPor,
    LocalDateTime fechaRegistro,

    String urlArchivo

) {
}
