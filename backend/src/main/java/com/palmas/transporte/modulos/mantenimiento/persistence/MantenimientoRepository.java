package com.palmas.transporte.modulos.mantenimiento.persistence;


import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;

public interface MantenimientoRepository
        extends JpaRepository<Mantenimiento, Long> {

    List<Mantenimiento> findAllByOrderByFechaReporteDesc();

    List<Mantenimiento>
        findByVehiculoIdOrderByFechaReporteDesc(Long vehiculoId);

    boolean existsByVehiculoIdAndEstadoIn(
        Long vehiculoId,
        Collection<EstadoMantenimiento> estados
    );

    boolean existsByCodigoIgnoreCase(String codigo);
}