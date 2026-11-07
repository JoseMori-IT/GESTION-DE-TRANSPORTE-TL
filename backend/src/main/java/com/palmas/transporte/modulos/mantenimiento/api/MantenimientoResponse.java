package com.palmas.transporte.modulos.mantenimiento.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoFalla;

public record MantenimientoResponse(

    Long id,
    String codigo,

    Long vehiculoId,
    String vehiculoCodigo,
    String vehiculoPlaca,
    String vehiculoDescripcion,

    Long reportadoPorId,
    String reportadoPorNombre,

    TipoFalla tipoFalla,
    PrioridadMantenimiento prioridad,
    EstadoMantenimiento estado,

    String titulo,
    String descripcion,
    Integer kilometrajeReporte,

    String diagnostico,
    String trabajoRealizado,
    String tallerProveedor,
    BigDecimal costo,

    LocalDateTime fechaReporte,
    LocalDateTime fechaInicioAtencion,
    LocalDateTime fechaReparacion,
    LocalDateTime fechaCierre,
    LocalDateTime fechaActualizacion,

    String observacionesCierre
) {
}