package com.palmas.transporte.modulos.operacion.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record RecorridoSolicitudRequest(@NotNull Long rutaId,@NotNull @Valid PlanViajeRequest planViaje) {}
