package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.SolicitudTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;

import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class CierreServicioTest {
    private final ServicioTransporteRepository servicios = mock(ServicioTransporteRepository.class);
    private final VehiculoRepository vehiculos = mock(VehiculoRepository.class);
    private final ConductorRepository conductores = mock(ConductorRepository.class);
    private final EntityManager em = mock(EntityManager.class);
    private final ServicioTransporteService api = new ServicioTransporteService(
        servicios, vehiculos, conductores, mock(RutaRepository.class), em, org.mockito.Mockito.mock(com.palmas.transporte.modulos.configuracion.api.BaseOperativa.class));

    private ServicioTransporte preparar(EstadoServicio estado) {
        var camion = new Vehiculo();
        camion.setId(1L);
        camion.setEstado(estado == EstadoServicio.EN_CURSO
            ? EstadoVehiculo.EN_SERVICIO : EstadoVehiculo.DISPONIBLE);
        var chofer = new Conductor();
        chofer.setId(9L);
        chofer.setEstado(estado == EstadoServicio.EN_CURSO
            ? EstadoConductor.EN_SERVICIO : EstadoConductor.DISPONIBLE);
        var copiloto = new Conductor();
        copiloto.setId(3L);
        copiloto.setEstado(chofer.getEstado());
        var s = new ServicioTransporte();
        s.setId(15L);
        s.setEstado(estado);
        s.setVehiculo(camion);
        s.setConductor(chofer);
        s.setCopiloto(copiloto);
        when(servicios.findById(15L)).thenReturn(Optional.of(s));
        when(servicios.save(s)).thenReturn(s);
        return s;
    }

    @Test
    void completarLiberaRecursosYRegistraLlegada() {
        var s = preparar(EstadoServicio.EN_CURSO);
        api.cambiarEstado(15L, EstadoServicio.COMPLETADO);
        assertEquals(EstadoVehiculo.DISPONIBLE, s.getVehiculo().getEstado());
        assertEquals(EstadoConductor.DISPONIBLE, s.getConductor().getEstado());
        assertEquals(EstadoConductor.DISPONIBLE, s.getCopiloto().getEstado());
        assertNotNull(s.getFechaHoraLlegadaReal());
        var orden = inOrder(em);
        orden.verify(em).refresh(s, LockModeType.PESSIMISTIC_WRITE);
        orden.verify(em).refresh(s.getVehiculo(), LockModeType.PESSIMISTIC_WRITE);
        orden.verify(em).refresh(s.getCopiloto(), LockModeType.PESSIMISTIC_WRITE);
        orden.verify(em).refresh(s.getConductor(), LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void cancelarEnCursoNoPuedeLiberarRecursos() {
        var s = preparar(EstadoServicio.EN_CURSO);
        assertThrows(IllegalArgumentException.class,()->api.cambiarEstado(15L,EstadoServicio.CANCELADO));
        assertEquals(EstadoVehiculo.EN_SERVICIO,s.getVehiculo().getEstado());
        assertEquals(EstadoConductor.EN_SERVICIO,s.getConductor().getEstado());
        verify(servicios,never()).save(any());
    }

    @Test
    void cancelarProgramadoNoInventaHorasReales() {
        var s = preparar(EstadoServicio.PROGRAMADO);
        api.cambiarEstado(15L, EstadoServicio.CANCELADO);
        assertNull(s.getFechaHoraSalidaReal());
        assertNull(s.getFechaHoraLlegadaReal());
        assertEquals(EstadoVehiculo.DISPONIBLE, s.getVehiculo().getEstado());
    }

    @Test
    void servicioCompletadoNoPuedeReabrirse() {
        preparar(EstadoServicio.COMPLETADO);
        assertThrows(IllegalArgumentException.class,
            () -> api.cambiarEstado(15L, EstadoServicio.EN_CURSO));
        verify(servicios, never()).save(any());
    }

    @Test
    void solicitudReflejaServicioActualSinModificarHistorial() {
        var solicitud = new SolicitudTransporte();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        assertEquals(EstadoSolicitud.APROBADA, SolicitudTransporteService.estadoVisible(solicitud));
        var servicio = new ServicioTransporte();
        solicitud.setServicio(servicio);
        solicitud.setEstado(EstadoSolicitud.PROGRAMADA);
        servicio.setEstado(EstadoServicio.EN_CURSO);
        assertEquals(EstadoSolicitud.EN_CURSO, SolicitudTransporteService.estadoVisible(solicitud));
        servicio.setEstado(EstadoServicio.COMPLETADO);
        assertEquals(EstadoSolicitud.COMPLETADA, SolicitudTransporteService.estadoVisible(solicitud));
        servicio.setEstado(EstadoServicio.CANCELADO);
        assertEquals(EstadoSolicitud.CANCELADA, SolicitudTransporteService.estadoVisible(solicitud));
        assertEquals(EstadoSolicitud.PROGRAMADA, solicitud.getEstado());
    }
}
