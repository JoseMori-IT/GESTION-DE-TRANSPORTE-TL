package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.*;

public record EstimacionTramoRequest(
    @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal distanciaKm,
    @NotNull @Min(1) @Max(10080) Integer duracionMinutos,
    @NotBlank @Pattern(regexp="CARRETERA|MANUAL") String fuente,
    @Size(max=50000) List<List<Double>> recorrido
) {
    @AssertTrue(message="El trazado vial debe contener coordenadas válidas")
    public boolean isRecorridoValido() {
        if (!"CARRETERA".equals(fuente)) return true;
        return recorrido!=null && recorrido.size()>=2 && recorrido.stream().allMatch(p -> p!=null && p.size()==2 && p.get(0)!=null && p.get(1)!=null && Double.isFinite(p.get(0)) && Double.isFinite(p.get(1)) && Math.abs(p.get(0))<=90 && Math.abs(p.get(1))<=180);
    }
    public String recorridoJson() {
        if (!"CARRETERA".equals(fuente) || !isRecorridoValido()) return null;
        return recorrido.stream().map(p -> "["+p.get(0)+","+p.get(1)+"]").collect(java.util.stream.Collectors.joining(",","[","]"));
    }
}
