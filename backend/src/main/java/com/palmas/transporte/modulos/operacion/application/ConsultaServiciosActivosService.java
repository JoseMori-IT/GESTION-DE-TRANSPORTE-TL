package com.palmas.transporte.modulos.operacion.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.palmas.transporte.modulos.operacion.api.ConsultaServiciosActivos;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

@Service
@Transactional(readOnly = true)
public class ConsultaServiciosActivosService implements ConsultaServiciosActivos {
    private static final List<EstadoServicio> ASIGNADOS = List.of(EstadoServicio.PROGRAMADO, EstadoServicio.EN_CURSO);
    private final ServicioTransporteRepository servicios;

    public ConsultaServiciosActivosService(ServicioTransporteRepository servicios) {
        this.servicios = servicios;
    }

    @Override
    public boolean unidadTieneAsignacion(Long vehiculoId) {
        return servicios.existsByVehiculoIdAndEstadoIn(vehiculoId, ASIGNADOS);
    }

    @Override
    public boolean unidadEnViaje(Long vehiculoId) {
        return servicios.existsByVehiculoIdAndEstadoIn(vehiculoId, List.of(EstadoServicio.EN_CURSO));
    }

    @Override
    public boolean personaTieneAsignacion(Long conductorId) {
        // Incluye asignaciones antiguas de copiloto para no liberar personal reservado.
        return servicios.existsByConductorIdAndEstadoIn(conductorId, ASIGNADOS)
            || servicios.existsByCopilotoIdAndEstadoIn(conductorId, ASIGNADOS);
    }

    @Override
    public List<String> codigosAsignadosAUnidad(Long vehiculoId) {
        return servicios.findByVehiculoIdAndEstadoIn(vehiculoId, ASIGNADOS).stream()
            .map(ServicioTransporte::getCodigo).sorted().toList();
    }
}
