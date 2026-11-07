package com.palmas.transporte.modulos.mantenimiento.api;


import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoFalla;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MantenimientoRequest(

    @NotNull(message = "El vehículo es obligatorio")
    Long vehiculoId,

    @NotNull(message = "El tipo de falla es obligatorio")
    TipoFalla tipoFalla,

    @NotNull(message = "La prioridad es obligatoria")
    PrioridadMantenimiento prioridad,

    @NotBlank(message = "El título es obligatorio")
    @Size(
        max = 150,
        message = "El título admite hasta 150 caracteres"
    )
    String titulo,

    @NotBlank(message = "La descripción es obligatoria")
    @Size(
        max = 1500,
        message = "La descripción admite hasta 1500 caracteres"
    )
    String descripcion,

    @Min(
        value = 0,
        message = "El kilometraje no puede ser negativo"
    )
    Integer kilometrajeReporte
) {
}