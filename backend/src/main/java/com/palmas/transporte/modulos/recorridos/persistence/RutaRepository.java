package com.palmas.transporte.modulos.recorridos.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.recorridos.domain.Ruta;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(
        String codigo,
        Long id
    );

    boolean existsByOrigenUbicacionIdAndDestinoUbicacionId(
        Long origenId,
        Long destinoId
    );

    boolean existsByOrigenUbicacionIdAndDestinoUbicacionIdAndIdNot(
        Long origenId,
        Long destinoId,
        Long id
    );
}
