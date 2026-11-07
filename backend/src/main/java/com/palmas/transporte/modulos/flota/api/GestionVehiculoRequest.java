package com.palmas.transporte.modulos.flota.api;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
public record GestionVehiculoRequest(@NotNull @Valid VehiculoRequest datos,
 @NotNull EstadoVehiculo estadoObjetivo, @Valid MantenimientoRequest mantenimiento) {}
