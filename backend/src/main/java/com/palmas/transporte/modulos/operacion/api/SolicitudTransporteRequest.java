package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.PrioridadSolicitud;
import com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudTransporteRequest(

    @NotNull(message = "El tipo de solicitud es obligatorio")
    TipoSolicitudTransporte tipoSolicitud,

    @NotBlank(message = "El nombre del solicitante es obligatorio")
    @Size(
        max = 150,
        message = "El nombre admite hasta 150 caracteres"
    )
    String solicitanteNombre,

    @NotBlank(message = "El área solicitante es obligatoria")
    @Size(
        max = 100,
        message = "El área admite hasta 100 caracteres"
    )
    String areaSolicitante,

    @Pattern(
        regexp = "^$|^[0-9+() -]{6,20}$",
        message = "El teléfono ingresado no es válido"
    )
    String telefono,

    @NotBlank(message = "El origen es obligatorio")
    @Size(
        max = 200,
        message = "El origen admite hasta 200 caracteres"
    )
    String origen,

    @NotBlank(message = "El destino es obligatorio")
    @Size(
        max = 200,
        message = "El destino admite hasta 200 caracteres"
    )
    String destino,

    @NotBlank(message = "El producto es obligatorio")
    @Pattern(
        regexp =
            "^(RFF|FS|ESCOBAJO|FIBRA|"
            + "TORTA_CLARIFICACION|LODO_BIOGAS|"
            + "CENIZA|RESIDUOS_VARIOS)$",
        message = "Selecciona un producto válido"
    )
    String productoTransportado,

    @Size(
        max = 500,
        message = "La descripción admite hasta 500 caracteres"
    )
    String descripcionProducto,

    @DecimalMin(
        value = "0.01",
        message = "La cantidad debe ser mayor que cero"
    )
    @Digits(
        integer = 8,
        fraction = 2,
        message = "La cantidad admite hasta dos decimales"
    )
    BigDecimal cantidadEstimadaToneladas,

    @NotNull(message = "La fecha requerida es obligatoria")
    @FutureOrPresent(
        message = "La fecha requerida no puede estar en el pasado"
    )
    LocalDateTime fechaRequerida,

    @NotNull(message = "La prioridad es obligatoria")
    PrioridadSolicitud prioridad,

    @Size(
        max = 700,
        message = "Las observaciones admiten hasta 700 caracteres"
    )
    String observaciones,

    @NotNull(message = "Selecciona o registra una ruta antes de enviar la solicitud") Long rutaId,
    @NotNull(message="Define el recorrido completo de la solicitud") @jakarta.validation.Valid PlanViajeRequest planViaje
) {
}