package com.palmas.transporte.modulos.operacion.api;


import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record ServicioRequest(

    @NotBlank(message = "El código es obligatorio")
    @Pattern(
        regexp = "^[A-Za-z0-9-]{3,30}$",
        message = "El código debe contener entre 3 y 30 caracteres"
    )
    String codigo,

    @NotNull(message = "El vehículo es obligatorio")
    Long vehiculoId,

    @NotNull(message = "El conductor es obligatorio")
    Long conductorId,

    @NotNull(message = "Selecciona una ruta antes de programar") Long rutaId,

    @NotBlank(message = "El origen es obligatorio")
    @Size(max = 200, message = "El origen admite hasta 200 caracteres")
    String origenDescripcion,

    @NotBlank(message = "El destino es obligatorio")
    @Size(max = 200, message = "El destino admite hasta 200 caracteres")
    String destinoDescripcion,

    Boolean requiereRetorno,

    
    @NotNull(message = "Indica la fecha y hora planificadas")
    @FutureOrPresent(
        message = "La salida no puede programarse en el pasado"
    )
    LocalDateTime fechaHoraSalida,

    
    @NotBlank(message = "El producto transportado es obligatorio")
@Size(
    max = 150,
    message = "El producto admite hasta 150 caracteres"
)

@Pattern(
    regexp = "^(RFF|FS|ESCOBAJO|FIBRA|TORTA_CLARIFICACION|LODO_BIOGAS|CENIZA|RESIDUOS_VARIOS)$",
    message = "Selecciona un producto transportado válido"
)

String productoTransportado,

    @NotNull(message = "El estado es obligatorio")
    EstadoServicio estado,

    @Size(
        max = 500,
        message = "Las observaciones admiten hasta 500 caracteres"
    )
    String observaciones,
    Long copilotoId,

@Size(
    max = 500,
    message = "La descripción admite hasta 500 caracteres"
)
String descripcionResiduos,

@jakarta.validation.Valid @Size(max = 30) java.util.List<TramoRequest> tramos,
@NotNull(message = "El viaje requiere un recorrido planificado") @jakarta.validation.Valid PlanViajeRequest planViaje
) {
    @AssertTrue(
        message = "Describe los residuos cuando selecciones RESIDUOS_VARIOS"
    )
    public boolean isDescripcionResiduosValida() {
        return !"RESIDUOS_VARIOS".equals(productoTransportado)
            || (descripcionResiduos != null
                && !descripcionResiduos.isBlank());
    }
}

 
