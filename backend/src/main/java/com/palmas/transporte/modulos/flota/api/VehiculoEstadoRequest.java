package com.palmas.transporte.modulos.flota.api;


import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;

import jakarta.validation.constraints.NotNull;

public record VehiculoEstadoRequest(

    @NotNull(message = "El estado del vehículo es obligatorio")
    EstadoVehiculo estado

) {
}