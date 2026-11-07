package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaServiciosActivosTest {
    private final ServicioTransporteRepository repo = mock(ServicioTransporteRepository.class);
    private final ConsultaServiciosActivosService consulta = new ConsultaServiciosActivosService(repo);

    @Test void laReservaIncluyeProgramadosYEnCurso() {
        var activos = List.of(EstadoServicio.PROGRAMADO, EstadoServicio.EN_CURSO);
        when(repo.existsByVehiculoIdAndEstadoIn(1L, activos)).thenReturn(true);
        assertTrue(consulta.unidadTieneAsignacion(1L));
        verify(repo).existsByVehiculoIdAndEstadoIn(1L, activos);
    }

    @Test void elAjusteDeOdometroSoloConsultaViajesEnCurso() {
        consulta.unidadEnViaje(1L);
        verify(repo).existsByVehiculoIdAndEstadoIn(1L, List.of(EstadoServicio.EN_CURSO));
    }

    @Test void conservaLasReservasAntiguasComoCopiloto() {
        var activos = List.of(EstadoServicio.PROGRAMADO, EstadoServicio.EN_CURSO);
        when(repo.existsByCopilotoIdAndEstadoIn(2L, activos)).thenReturn(true);
        assertTrue(consulta.personaTieneAsignacion(2L));
    }
}
