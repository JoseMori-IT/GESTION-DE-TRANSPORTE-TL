package com.palmas.transporte.modulos.operacion.persistence;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.operacion.domain.MarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;

public interface MarcacionServicioRepository
        extends JpaRepository<MarcacionServicio, Long> {

    Optional<MarcacionServicio> findByEventoUuid(String eventoUuid);

    List<MarcacionServicio> findByServicioIdOrderByFechaHoraDispositivoAsc(
        Long servicioId
    );

    boolean existsByServicioIdAndTipo(
        Long servicioId,
        TipoMarcacionServicio tipo
    );
}
