package com.palmas.transporte.modulos.identidad.application;

import com.palmas.transporte.modulos.identidad.application.ConductorService;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.palmas.transporte.modulos.identidad.api.ConductorRequest;



class ConductorEstadoTest {
    private final ConductorRepository personal = mock(ConductorRepository.class);
    private final ServicioTransporteRepository servicios = mock(ServicioTransporteRepository.class);
    private final ConductorService api = new ConductorService(personal, new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(servicios));

    private Conductor preparar(EstadoConductor estado) {
        Conductor c = new Conductor();
        c.setId(2L);
        c.setEstado(estado);
        c.setRolTripulacion(RolTripulacion.CHOFER);
        when(personal.bloquearPorId(2L)).thenReturn(Optional.of(c));
        when(personal.save(c)).thenReturn(c);
        return c;
    }

    @Test
    void rechazaDesactivarChoferReservado() {
        Conductor c = preparar(EstadoConductor.DISPONIBLE);
        when(servicios.existsByConductorIdAndEstadoIn(eq(2L), any())).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> api.desactivar(2L));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(EstadoConductor.DISPONIBLE, c.getEstado());
        verify(personal, never()).save(any());
    }

    @Test
    void rechazaDesactivarCopilotoReservado() {
        preparar(EstadoConductor.DISPONIBLE);
        when(servicios.existsByCopilotoIdAndEstadoIn(eq(2L), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> api.desactivar(2L));
        verify(personal, never()).save(any());
    }

    @Test
    void permiteDesactivarPersonalLibre() {
        preparar(EstadoConductor.DISPONIBLE);
        assertEquals(EstadoConductor.INACTIVO, api.desactivar(2L).getEstado());
    }

    @Test
    void rechazaDesactivarPersonalEnServicio() {
        preparar(EstadoConductor.EN_SERVICIO);
        assertThrows(ResponseStatusException.class, () -> api.desactivar(2L));
        verify(personal, never()).save(any());
    }

    @Test
    void rechazaCrearPersonalEnServicio() {
        var request = mock(ConductorRequest.class);
        when(request.estado()).thenReturn(EstadoConductor.EN_SERVICIO);
        var error = assertThrows(ResponseStatusException.class, () -> api.registrar(request));
        assertEquals(400, error.getStatusCode().value());
        verify(personal, never()).save(any());
    }

    @Test
    void rechazaSuspenderPersonalReservadoDesdeEdicion() {
        preparar(EstadoConductor.DISPONIBLE);
        var request = mock(ConductorRequest.class);
        when(request.estado()).thenReturn(EstadoConductor.SUSPENDIDO);
        when(request.rolTripulacion()).thenReturn(RolTripulacion.CHOFER);
        when(servicios.existsByConductorIdAndEstadoIn(eq(2L), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> api.actualizar(2L, request));
        verify(personal, never()).save(any());
    }

    @Test
    void rechazaCambiarRolDePersonalReservado() {
        preparar(EstadoConductor.DISPONIBLE);
        var request = mock(ConductorRequest.class);
        when(request.estado()).thenReturn(EstadoConductor.DISPONIBLE);
        when(request.rolTripulacion()).thenReturn(RolTripulacion.COPILOTO);
        when(servicios.existsByConductorIdAndEstadoIn(eq(2L), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> api.actualizar(2L, request));
        verify(personal, never()).save(any());
    }

    @Test
    void permiteEditarDatosPersonalesSinCambiarEstadoNiRol() {
        Conductor c = preparar(EstadoConductor.DISPONIBLE);
        c.setRolTripulacion(RolTripulacion.COPILOTO);
        var request = mock(ConductorRequest.class);
        when(request.estado()).thenReturn(EstadoConductor.DISPONIBLE);
        when(request.rolTripulacion()).thenReturn(RolTripulacion.COPILOTO);
        when(request.dni()).thenReturn("12345678");
        when(request.nombres()).thenReturn("Nombre actualizado");
        when(request.apellidos()).thenReturn("Apellido");
        when(request.telefono()).thenReturn("987654321");
        assertEquals("Nombre actualizado", api.actualizar(2L, request).getNombres());
        verify(personal).save(c);
        verifyNoInteractions(servicios);
    }
}
