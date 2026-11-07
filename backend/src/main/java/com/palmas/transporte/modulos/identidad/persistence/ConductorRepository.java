package com.palmas.transporte.modulos.identidad.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.palmas.transporte.modulos.identidad.domain.Conductor;

import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;

public interface ConductorRepository
        extends JpaRepository<Conductor, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conductor c where c.id = :id")
    Optional<Conductor> bloquearPorId(@Param("id") Long id);

    boolean existsByDni(String dni);

    boolean existsByNumeroLicenciaIgnoreCase(String numeroLicencia);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByNumeroLicenciaIgnoreCaseAndIdNot(
        String numeroLicencia,
        Long id
    );

    boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(
    TipoDocumento tipoDocumento,
    String numeroDocumento
);

boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCaseAndIdNot(
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    Long id
);

}
