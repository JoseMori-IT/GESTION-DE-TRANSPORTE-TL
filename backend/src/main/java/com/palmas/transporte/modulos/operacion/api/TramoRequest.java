package com.palmas.transporte.modulos.operacion.api;


import jakarta.validation.constraints.*;

public record TramoRequest(
    @NotNull Long rutaId,
    @NotBlank @Pattern(regexp = "RFF|FS|ESCOBAJO|FIBRA|TORTA_CLARIFICACION|LODO_BIOGAS|CENIZA|RESIDUOS_VARIOS|SIN_CARGA") String productoTransportado,
    @NotBlank @Pattern(regexp = "RECOJO|ENTREGA|RETORNO|PASO") String actividad,
    @Size(max = 300) String indicaciones
) {}
