package com.palmas.transporte.modulos.operacion.persistence;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.operacion.domain.EvidenciaServicio;

public interface EvidenciaServicioRepository
        extends JpaRepository<
            EvidenciaServicio,
            Long
        > {

    /*
     * Consulta administrativa y operativa.
     */
    List<EvidenciaServicio>
        findByServicio_IdOrderByFechaRegistroDesc(
            Long servicioId
        );

    /*
     * Consulta restringida para el chofer autenticado.
     */
    List<EvidenciaServicio>
        findByServicio_IdAndServicio_Conductor_IdOrderByFechaRegistroDesc(
            Long servicioId,
            Long conductorId
        );

    /*
     * Descarga restringida para el chofer autenticado.
     */
    Optional<EvidenciaServicio>
        findByIdAndServicio_Conductor_Id(
            Long evidenciaId,
            Long conductorId
        );

    boolean existsByServicio_Id(
        Long servicioId
    );
    Optional<EvidenciaServicio> findByNombreArchivoStartingWith(String prefijo);
}
