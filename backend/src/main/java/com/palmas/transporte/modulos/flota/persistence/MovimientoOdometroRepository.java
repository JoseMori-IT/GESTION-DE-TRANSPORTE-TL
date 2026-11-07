package com.palmas.transporte.modulos.flota.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.palmas.transporte.modulos.flota.domain.MovimientoOdometro;
public interface MovimientoOdometroRepository extends JpaRepository<MovimientoOdometro,Long>{
 List<MovimientoOdometro> findByVehiculoIdOrderByIdDesc(Long id);
}
