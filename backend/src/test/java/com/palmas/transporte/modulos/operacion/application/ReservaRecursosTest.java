package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import com.palmas.transporte.modulos.operacion.api.ServicioRequest;



class ReservaRecursosTest {
    @Test
    void bloqueaRecursosEnOrdenAntesDeConsultarReservas() {
        var vehiculos = mock(VehiculoRepository.class);
        var conductores = mock(ConductorRepository.class);
        var servicios = mock(ServicioTransporteRepository.class);
        var request = mock(ServicioRequest.class);
        when(request.fechaHoraSalida()).thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(request.planViaje()).thenReturn(mock(com.palmas.transporte.modulos.operacion.api.PlanViajeRequest.class));
        when(request.rutaId()).thenReturn(1L);
        when(request.vehiculoId()).thenReturn(1L);
        when(request.conductorId()).thenReturn(9L);
        when(request.copilotoId()).thenReturn(null);
        when(request.codigo()).thenReturn("TEST");
        when(request.estado()).thenReturn(EstadoServicio.COMPLETADO);
        when(vehiculos.bloquearPorId(1L)).thenReturn(Optional.of(new Vehiculo()));
        when(conductores.bloquearPorId(9L)).thenReturn(Optional.of(new Conductor()));
        var api = new ServicioTransporteService(servicios, vehiculos, conductores,
            mock(RutaRepository.class), mock(jakarta.persistence.EntityManager.class), org.mockito.Mockito.mock(com.palmas.transporte.modulos.configuracion.api.BaseOperativa.class));
        assertThrows(IllegalArgumentException.class, () -> api.registrar(request));
        var orden = inOrder(vehiculos, conductores, servicios);
        orden.verify(vehiculos).bloquearPorId(1L);
        orden.verify(conductores).bloquearPorId(9L);
        orden.verify(servicios).existsByCodigoIgnoreCase("TEST");
        verify(servicios, never()).save(any());
    }
}

