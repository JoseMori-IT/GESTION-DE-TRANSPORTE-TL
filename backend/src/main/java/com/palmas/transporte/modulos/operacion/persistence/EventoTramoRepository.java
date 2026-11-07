package com.palmas.transporte.modulos.operacion.persistence;


import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.palmas.transporte.modulos.operacion.domain.EventoTramo;

public interface EventoTramoRepository extends JpaRepository<EventoTramo, Long> {
    Optional<EventoTramo> findByEventoUuid(String uuid);
    List<EventoTramo> findByTramoServicioIdOrderByFechaHoraDispositivoAsc(Long servicioId);
}
