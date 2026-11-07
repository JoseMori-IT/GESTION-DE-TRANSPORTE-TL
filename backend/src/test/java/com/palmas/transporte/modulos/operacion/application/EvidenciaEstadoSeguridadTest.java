package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EvidenciaServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoEvidencia;
import com.palmas.transporte.modulos.operacion.persistence.EvidenciaServicioRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;



class EvidenciaEstadoSeguridadTest {
    @TempDir Path directorio;
    @Test void reintentoConfirmadoNoDuplicaFotoNiReabreViajeCompletado() {
        var evidencias = mock(EvidenciaServicioRepository.class);
        var servicios = mock(ServicioTransporteRepository.class);
        var api = new EvidenciaServicioService(evidencias, servicios, directorio.toString());
        var servicio = new ServicioTransporte(); servicio.setId(1L); servicio.setEstado(EstadoServicio.COMPLETADO);
        var previa = new EvidenciaServicio(); previa.setId(7L); previa.setServicio(servicio); previa.setTipo(TipoEvidencia.DOCUMENTO);
        String uuid = "dde07faa-1466-4323-84d8-3444ab72b002";
        when(servicios.bloquearEvidencias(1L)).thenReturn(Optional.of(servicio));
        when(evidencias.findByNombreArchivoStartingWith("offline-" + uuid + ".")).thenReturn(Optional.of(previa));
        var respuesta = api.registrar(1L, TipoEvidencia.DOCUMENTO, null, null, null, null, null, null, "Prueba", "MASTER", null, uuid);
        assertEquals(7L, respuesta.id());
        verify(evidencias, never()).saveAndFlush(any());
    }

    @Test void rechazaCargaFueraDeViajeEnCursoInclusoParaMaster() {
        var evidencias = mock(EvidenciaServicioRepository.class);
        var servicios = mock(ServicioTransporteRepository.class);
        var api = new EvidenciaServicioService(evidencias, servicios, directorio.toString());
        for (var estado : new EstadoServicio[]{EstadoServicio.PROGRAMADO, EstadoServicio.COMPLETADO, EstadoServicio.CANCELADO}) {
            var servicio = new ServicioTransporte();
            servicio.setEstado(estado);
            when(servicios.findById(1L)).thenReturn(Optional.of(servicio));
            var error = assertThrows(ResponseStatusException.class, () -> api.registrar(
                1L, null, null, null, null, null, null, null, "Prueba", "MASTER", null));
            assertEquals(409, error.getStatusCode().value());
        }
        verifyNoInteractions(evidencias);
    }
    @Test void conformidadSeRegistraSeparadaDeEvidencias() {
        var evidencias=mock(EvidenciaServicioRepository.class);
        var servicios=mock(ServicioTransporteRepository.class);
        var api=new EvidenciaServicioService(evidencias,servicios,directorio.toString());
        var e=assertThrows(ResponseStatusException.class,()->api.registrar(1L,TipoEvidencia.CONFORMIDAD,null,null,null,null,"Nombre","12345678","Chofer","CHOFER",1L));
        assertEquals(409,e.getStatusCode().value());
        verifyNoInteractions(evidencias);
    }
}
