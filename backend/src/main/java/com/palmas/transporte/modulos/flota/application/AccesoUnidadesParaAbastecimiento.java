package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.api.UnidadesParaAbastecimiento;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccesoUnidadesParaAbastecimiento implements UnidadesParaAbastecimiento {
    private final VehiculoRepository unidades;

    public AccesoUnidadesParaAbastecimiento(VehiculoRepository unidades) {
        this.unidades = unidades;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Unidad bloquear(Long id) {
        var v = unidades.bloquearPorId(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.CONFLICT, "Unidad no encontrada."));
        if (v.getEstado() == EstadoVehiculo.BAJA_DEFINITIVA || v.getEstado() == EstadoVehiculo.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede abastecer una unidad inactiva o dada de baja.");
        }
        return new Unidad(v.getId(), v.getPlaca(), v.getEstadoOdometro(),
            v.getOdometroKm(), v.getFechaOdometro());
    }
}
