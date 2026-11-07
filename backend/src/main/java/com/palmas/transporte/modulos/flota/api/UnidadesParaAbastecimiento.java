package com.palmas.transporte.modulos.flota.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contrato de Flota: lectura coherente de una unidad, sin exponer entidades mutables. */
public interface UnidadesParaAbastecimiento {
    Unidad bloquear(Long id);

    record Unidad(Long id, String placa, String estadoOdometro,
                  BigDecimal odometroKm, LocalDateTime fechaOdometro) {}
}
