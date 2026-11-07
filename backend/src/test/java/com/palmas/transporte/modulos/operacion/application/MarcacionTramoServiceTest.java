package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.flota.application.OdometroService;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoOdometroRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EventoTramo;
import com.palmas.transporte.modulos.operacion.domain.IncidenciaViaje;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.persistence.EventoTramoRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




class MarcacionTramoServiceTest {
    private final EventoTramoRepository eventos=mock(EventoTramoRepository.class);
    private final ServicioTransporteRepository servicios=mock(ServicioTransporteRepository.class);
    private final ServicioTransporteService estados=mock(ServicioTransporteService.class);
    private final MarcacionTramoService api=new MarcacionTramoService(eventos,servicios,estados,mock(EntityManager.class),new OdometroService(mock(MovimientoOdometroRepository.class),new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(servicios),mock(VehiculoRepository.class)),mock(IncidenciaViajeService.class));
    private ServicioTransporte viaje;
    private final LocalDateTime hora=LocalDateTime.now().minusHours(2);
    @BeforeEach void preparar() {
        viaje=new ServicioTransporte();viaje.setId(10L);viaje.setEstado(EstadoServicio.PROGRAMADO);viaje.setVehiculo(new Vehiculo());
        for(int i=1;i<=2;i++){var t=new TramoServicio();t.setId((long)i);t.setOrden(i);t.setServicio(viaje);viaje.getTramos().add(t);}
        when(servicios.findByIdAndConductorId(10L,5L)).thenReturn(Optional.of(viaje));
        when(eventos.findByEventoUuid(any())).thenReturn(Optional.empty());when(eventos.save(any())).thenAnswer(inv->inv.getArgument(0));
        when(estados.cambiarEstadoDesdeMarcacion(any(),any())).thenAnswer(inv->{viaje.setEstado(inv.getArgument(1));return viaje;});
    }
    private MarcacionServicioRequest evento(int orden,TipoMarcacionServicio tipo,int minutos) {
        return new MarcacionServicioRequest(UUID.randomUUID().toString(),tipo,new BigDecimal("-8.4"),new BigDecimal("-74.8"),BigDecimal.TEN,hora.plusMinutes(minutos),true,null,orden,orden==1&&tipo==TipoMarcacionServicio.SALIDA_IDA?new BigDecimal("12500"):orden==2&&tipo==TipoMarcacionServicio.LLEGADA_DESTINO?new BigDecimal("12600"):null);
    }
    @Test void soloFinalizaYLiberaAlLlegarAlUltimoDestino() {
        api.registrar(10L,5L,evento(1,TipoMarcacionServicio.SALIDA_IDA,0));
        api.registrar(10L,5L,evento(1,TipoMarcacionServicio.LLEGADA_DESTINO,20));
        verify(estados,never()).cambiarEstadoDesdeMarcacion(10L,EstadoServicio.COMPLETADO);
        assertEquals(EstadoServicio.EN_CURSO,viaje.getEstado());
        api.registrar(10L,5L,evento(2,TipoMarcacionServicio.SALIDA_IDA,30));
        api.registrar(10L,5L,evento(2,TipoMarcacionServicio.LLEGADA_DESTINO,60));
        verify(estados).cambiarEstadoDesdeMarcacion(10L,EstadoServicio.COMPLETADO);assertEquals(hora.plusMinutes(60),viaje.getFechaHoraLlegadaReal());
    }
    @Test void rechazaSaltarParadasYHorasIncoherentes() {
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,evento(2,TipoMarcacionServicio.SALIDA_IDA,0)));
        api.registrar(10L,5L,evento(1,TipoMarcacionServicio.SALIDA_IDA,10));
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,evento(1,TipoMarcacionServicio.LLEGADA_DESTINO,0)));
    }
    @Test void reintentoConfirmadoNoDuplicaEventosNiModificaElAvance() {
        var r=evento(1,TipoMarcacionServicio.SALIDA_IDA,0);var e=new EventoTramo();e.setTramo(viaje.getTramos().get(0));e.setTipo(r.tipo());e.setEventoUuid(r.eventoUuid());
        when(eventos.findByEventoUuid(r.eventoUuid())).thenReturn(Optional.of(e));
        assertEquals(1,api.registrar(10L,5L,r).tramoOrden());verify(eventos,never()).save(any());verifyNoInteractions(estados);
    }
    @Test void impideMarcarUnViajeDeOtroChofer() {
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,99L,evento(1,TipoMarcacionServicio.SALIDA_IDA,0)));verifyNoInteractions(eventos);
    }
    private MarcacionServicioRequest lectura(int orden,TipoMarcacionServicio tipo,int minutos,String km) {
        var r=evento(orden,tipo,minutos);
        return new MarcacionServicioRequest(r.eventoUuid(),r.tipo(),r.latitud(),r.longitud(),r.precisionMetros(),r.fechaHoraDispositivo(),r.registradoOffline(),r.observacion(),r.tramoOrden(),new BigDecimal(km));
    }
    @Test void conservaOdometrosYRechazaRegresoMenorQueSalida() {
        viaje.getTramos().forEach(t->t.setFuenteDistancia("CARRETERA"));
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,new MarcacionServicioRequest(UUID.randomUUID().toString(),TipoMarcacionServicio.SALIDA_IDA,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,hora,true,null,1)));
        api.registrar(10L,5L,lectura(1,TipoMarcacionServicio.SALIDA_IDA,0,"12500"));
        api.registrar(10L,5L,evento(1,TipoMarcacionServicio.LLEGADA_DESTINO,20));
        api.registrar(10L,5L,evento(2,TipoMarcacionServicio.SALIDA_IDA,30));
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,lectura(2,TipoMarcacionServicio.LLEGADA_DESTINO,60,"12499")));
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,lectura(2,TipoMarcacionServicio.LLEGADA_DESTINO,60,"12500")));
        assertNull(viaje.getTramos().get(1).getLlegadaReal());
        api.registrar(10L,5L,lectura(2,TipoMarcacionServicio.LLEGADA_DESTINO,60,"12615.5"));
        assertEquals(new BigDecimal("12500"),viaje.getTramos().get(0).getOdometroSalidaKm());
        assertEquals(new BigDecimal("12615.5"),viaje.getTramos().get(1).getOdometroLlegadaKm());
    }

    @Test void averiaAbiertaImpideSeguirMarcandoParadas(){
        viaje.setEstado(EstadoServicio.EN_CURSO);
        var i=new IncidenciaViaje();i.setEstado("REPORTADA");viaje.getIncidencias().add(i);
        assertThrows(ResponseStatusException.class,()->api.registrar(10L,5L,new MarcacionServicioRequest(UUID.randomUUID().toString(),TipoMarcacionServicio.SALIDA_IDA,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,hora,true,null,1)));
        verify(eventos,never()).save(any());
    }
}
