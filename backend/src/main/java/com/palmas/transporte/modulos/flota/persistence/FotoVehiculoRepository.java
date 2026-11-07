package com.palmas.transporte.modulos.flota.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import com.palmas.transporte.modulos.flota.domain.FotoVehiculo;

public interface FotoVehiculoRepository
        extends JpaRepository<FotoVehiculo, Long> {
}
