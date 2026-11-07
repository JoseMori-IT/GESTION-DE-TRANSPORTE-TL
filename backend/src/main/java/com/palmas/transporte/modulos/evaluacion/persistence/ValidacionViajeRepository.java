package com.palmas.transporte.modulos.evaluacion.persistence;

import com.palmas.transporte.modulos.evaluacion.domain.ValidacionViaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ValidacionViajeRepository extends JpaRepository<ValidacionViaje,Long> {
 List<ValidacionViaje> findAllByOrderByIdDesc();
 java.util.Optional<ValidacionViaje> findFirstByServicioIdOrderByIdDesc(Long servicioId);
}
