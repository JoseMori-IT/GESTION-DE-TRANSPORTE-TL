package com.palmas.transporte.modulos.operacion.persistence;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;

public interface ServicioTransporteRepository
        extends JpaRepository<ServicioTransporte, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from ServicioTransporte s where s.id = :id")
    Optional<ServicioTransporte> bloquearEvidencias(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(
        String codigo,
        Long id
    );

    List<ServicioTransporte>
        findByConductorIdOrderByIdDesc(
            Long conductorId
        );

    Optional<ServicioTransporte>
        findByIdAndConductorId(
            Long id,
            Long conductorId
        );

    List<ServicioTransporte> findByVehiculoIdAndEstadoIn(Long vehiculoId, Collection<EstadoServicio> estados);

    boolean existsByVehiculoIdAndEstadoIn(
        Long vehiculoId,
        Collection<EstadoServicio> estados
    );

    boolean existsByConductorIdAndEstadoIn(
        Long conductorId,
        Collection<EstadoServicio> estados
    );

    boolean existsByCopilotoIdAndEstadoIn(
        Long copilotoId,
        Collection<EstadoServicio> estados
    );
}
