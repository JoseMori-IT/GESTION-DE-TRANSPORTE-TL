package com.palmas.transporte.modulos.evaluacion.persistence;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.evaluacion.domain.EncuestaServicio;

public interface EncuestaServicioRepository
        extends JpaRepository<EncuestaServicio, Long> {

    Optional<EncuestaServicio> findByServicioId(
        Long servicioId
    );

    boolean existsByServicioId(
        Long servicioId
    );
}