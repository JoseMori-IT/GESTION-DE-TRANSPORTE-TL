package com.palmas.transporte.modulos.mantenimiento.persistence;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.mantenimiento.domain.EvidenciaMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;

public interface EvidenciaMantenimientoRepository
        extends JpaRepository<
            EvidenciaMantenimiento,
            Long
        > {

    List<EvidenciaMantenimiento>
        findByMantenimiento_IdOrderByFechaRegistroDesc(
            Long mantenimientoId
        );

    boolean existsByMantenimiento_IdAndTipo(
        Long mantenimientoId,
        TipoEvidenciaMantenimiento tipo
    );

    long countByMantenimiento_Id(
        Long mantenimientoId
    );
}