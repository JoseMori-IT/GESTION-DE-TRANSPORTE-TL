package com.palmas.transporte.modulos.flota.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.palmas.transporte.modulos.flota.domain.Vehiculo;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.id = :id")
    Optional<Vehiculo> bloquearPorId(@Param("id") Long id);

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByCodigoInternoIgnoreCase(String codigoInterno);

    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);

    boolean existsByCodigoInternoIgnoreCaseAndIdNot(
        String codigoInterno,
        Long id
    );
}
