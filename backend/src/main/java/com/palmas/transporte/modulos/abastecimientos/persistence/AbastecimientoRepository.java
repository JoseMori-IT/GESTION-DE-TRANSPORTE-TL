package com.palmas.transporte.modulos.abastecimientos.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmas.transporte.modulos.abastecimientos.domain.Abastecimiento;
import java.util.*;
public interface AbastecimientoRepository extends JpaRepository<Abastecimiento,Long>{
 Optional<Abastecimiento> findByEventoUuid(String uuid);
 List<Abastecimiento> findByVehiculoIdAndAnuladoFalseOrderByFechaAsc(Long vehiculoId);
 List<Abastecimiento> findAllByOrderByFechaDescIdDesc();
}
