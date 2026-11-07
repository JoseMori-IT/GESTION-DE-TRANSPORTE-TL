package com.palmas.transporte.modulos.auditoria.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.palmas.transporte.modulos.auditoria.domain.Auditoria;

public interface AuditoriaRepository
        extends JpaRepository<Auditoria, Long>,
        JpaSpecificationExecutor<Auditoria> {
}