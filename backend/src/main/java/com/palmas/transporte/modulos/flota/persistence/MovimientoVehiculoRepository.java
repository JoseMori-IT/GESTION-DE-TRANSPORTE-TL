package com.palmas.transporte.modulos.flota.persistence;

import com.palmas.transporte.modulos.flota.domain.MovimientoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MovimientoVehiculoRepository extends JpaRepository<MovimientoVehiculo,Long>{
 List<MovimientoVehiculo> findByVehiculoIdOrderByIdDesc(Long vehiculoId);
 Optional<MovimientoVehiculo> findFirstByVehiculoIdOrderByIdDesc(Long vehiculoId);
}
