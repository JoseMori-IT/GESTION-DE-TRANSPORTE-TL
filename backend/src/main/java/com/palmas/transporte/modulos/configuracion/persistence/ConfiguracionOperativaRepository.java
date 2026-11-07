package com.palmas.transporte.modulos.configuracion.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import com.palmas.transporte.modulos.configuracion.domain.ConfiguracionOperativa;
public interface ConfiguracionOperativaRepository extends JpaRepository<ConfiguracionOperativa,Long> {}
