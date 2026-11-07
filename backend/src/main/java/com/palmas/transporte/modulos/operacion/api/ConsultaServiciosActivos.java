package com.palmas.transporte.modulos.operacion.api;

import java.util.List;

/** Consultas operativas sin exponer entidades ni repositorios del módulo. */
public interface ConsultaServiciosActivos {
    boolean unidadTieneAsignacion(Long vehiculoId);
    boolean unidadEnViaje(Long vehiculoId);
    boolean personaTieneAsignacion(Long conductorId);
    List<String> codigosAsignadosAUnidad(Long vehiculoId);
}
