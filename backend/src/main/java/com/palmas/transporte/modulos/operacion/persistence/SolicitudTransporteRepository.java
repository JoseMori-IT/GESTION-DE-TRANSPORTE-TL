package com.palmas.transporte.modulos.operacion.persistence;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;
import com.palmas.transporte.modulos.operacion.domain.SolicitudTransporte;

public interface SolicitudTransporteRepository
        extends JpaRepository<SolicitudTransporte, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<SolicitudTransporte> findAllByOrderByIdDesc();

    List<SolicitudTransporte>
        findByEstadoOrderByFechaRequeridaAsc(
            EstadoSolicitud estado
        );
}