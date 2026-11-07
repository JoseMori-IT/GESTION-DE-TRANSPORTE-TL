package com.palmas.transporte.modulos.configuracion.api;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ConfiguracionOperativaResponse(Long version,String sede,Long plantaBaseId,String plantaBaseNombre,
 BigDecimal galonesPorKm,BigDecimal kgCo2PorKm,BigDecimal litrosPorGalon,String unidadGalon,
 String referenciaCombustible,String referenciaEmisiones,String actualizadoPor,LocalDateTime actualizadoEn) {}
