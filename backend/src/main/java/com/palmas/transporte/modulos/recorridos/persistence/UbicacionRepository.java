package com.palmas.transporte.modulos.recorridos.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;

public interface UbicacionRepository
        extends JpaRepository<Ubicacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(
        String nombre,
        Long id
    );

    boolean existsByCodigoIgnoreCase(String codigo);
}
