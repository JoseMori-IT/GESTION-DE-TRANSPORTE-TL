package com.palmas.transporte.modulos.operacion.persistence;

import com.palmas.transporte.modulos.operacion.domain.IncidenciaViaje;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IncidenciaViajeRepository extends JpaRepository<IncidenciaViaje,Long>{}
