package com.palmas.transporte.modulos.operacion.api;


import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PlanViajeRequest(
    @NotNull Long inicioUbicacionId,
    @NotNull Long plantaRetornoId,
    @NotEmpty @Size(max = 29) List<@NotNull @Valid Parada> paradas,
    @Size(max = 30) List<@NotNull @Valid EstimacionTramoRequest> estimaciones
) {
    public PlanViajeRequest(Long inicioUbicacionId,Long plantaRetornoId,List<Parada> paradas) { this(inicioUbicacionId,plantaRetornoId,paradas,null); }
    public record Parada(
        @NotNull Long ubicacionId,
        @NotBlank @Pattern(regexp = "RECOJO|ENTREGA|PASO") String actividad,
        @NotBlank @Pattern(regexp = "RFF|FS|ESCOBAJO|FIBRA|TORTA_CLARIFICACION|LODO_BIOGAS|CENIZA|RESIDUOS_VARIOS|SIN_CARGA") String producto,
        @Size(max = 300) String indicaciones
    ) {}
}
