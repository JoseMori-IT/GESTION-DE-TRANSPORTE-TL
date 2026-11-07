package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import com.palmas.transporte.modulos.operacion.api.EstimacionTramoRequest;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.api.ProgramarSolicitudRequest;
import com.palmas.transporte.modulos.operacion.api.RecorridoSolicitudRequest;
import com.palmas.transporte.modulos.operacion.api.ServicioRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudTransporteRequest;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;
import com.palmas.transporte.modulos.operacion.domain.PlanSolicitudConverter;
import com.palmas.transporte.modulos.operacion.domain.PrioridadSolicitud;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.SolicitudTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte;
import com.palmas.transporte.modulos.operacion.persistence.SolicitudTransporteRepository;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class SolicitudRutaTest {
    private final SolicitudTransporteRepository solicitudes=mock(SolicitudTransporteRepository.class);
    private final UsuarioRepository usuarios=mock(UsuarioRepository.class);
    private final RutaRepository rutas=mock(RutaRepository.class);
    private final UbicacionRepository puntos=mock(UbicacionRepository.class);
    private final ServicioTransporteService servicios=mock(ServicioTransporteService.class);
    private final SolicitudTransporteService api=new SolicitudTransporteService(solicitudes,usuarios,servicios,rutas,puntos, org.mockito.Mockito.mock(com.palmas.transporte.modulos.configuracion.api.BaseOperativa.class));
    private Ruta ruta;
    private Usuario usuario;
    private PlanViajeRequest plan;
    @BeforeEach void preparar(){
        usuario=new Usuario();usuario.setId(1L);usuario.setCorreo("prueba@example.test");
        var planta=new Ubicacion();planta.setId(1L);planta.setNombre("Tulumayo");planta.setTipo(TipoUbicacion.PLANTA);planta.setEstado(EstadoRuta.ACTIVA);
        var campo=new Ubicacion();campo.setId(2L);campo.setNombre("Neshuya");campo.setTipo(TipoUbicacion.PARCELA);campo.setEstado(EstadoRuta.ACTIVA);
        ruta=new Ruta();ruta.setId(2L);ruta.setEstado(EstadoRuta.ACTIVA);ruta.setOrigen("Tulumayo");ruta.setDestino("Neshuya");ruta.setOrigenUbicacion(planta);ruta.setDestinoUbicacion(campo);
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(usuario));when(rutas.findById(2L)).thenReturn(Optional.of(ruta));
        when(puntos.findById(1L)).thenReturn(Optional.of(planta));when(puntos.findById(2L)).thenReturn(Optional.of(campo));
        when(solicitudes.save(any())).thenAnswer(inv->inv.getArgument(0));
        plan=new PlanViajeRequest(1L,1L,List.of(new PlanViajeRequest.Parada(2L,"RECOJO","RFF",null)),List.of(new EstimacionTramoRequest(new BigDecimal("40"),50,"MANUAL",List.of()),new EstimacionTramoRequest(new BigDecimal("45"),55,"MANUAL",List.of())));
    }
    private SolicitudTransporteRequest request(PlanViajeRequest plan){return new SolicitudTransporteRequest(TipoSolicitudTransporte.RECOJO_RFF,"Prueba","Operaciones",null,"Texto distinto","Destino distinto","RFF",null,null,LocalDateTime.now().plusDays(1),PrioridadSolicitud.NORMAL,null,2L,plan);}
    private SolicitudTransporte aprobada(){var s=new SolicitudTransporte();s.setId(1L);s.setCodigo("SOL-001");s.setUsuarioSolicitante(usuario);s.setEstado(EstadoSolicitud.APROBADA);s.setRuta(ruta);s.setPlanViaje(plan);when(solicitudes.findById(1L)).thenReturn(Optional.of(s));return s;}
    private ProgramarSolicitudRequest programacion(PlanViajeRequest p){return new ProgramarSolicitudRequest("SER-001",1L,2L,null,2L,true,LocalDateTime.now().plusDays(1),null,List.of(),p);}
    @Test void guardaRecorridoCompletoConLaSolicitud(){
        var respuesta=api.registrar(request(plan),usuario.getCorreo());
        assertEquals("Tulumayo",respuesta.origen());assertEquals("Neshuya",respuesta.destino());assertEquals(plan,respuesta.planViaje());
        assertEquals("Tulumayo → Neshuya → Tulumayo",respuesta.resumenRecorrido());assertEquals(new BigDecimal("85"),respuesta.distanciaEstimadaKm());assertEquals(105,respuesta.duracionEstimadaMinutos());
    }
    @Test void exigeRecorridoYEstimacionesAntesDeRegistrar(){
        assertThrows(IllegalArgumentException.class,()->api.registrar(request(null),usuario.getCorreo()));
        assertThrows(IllegalArgumentException.class,()->api.registrar(request(new PlanViajeRequest(1L,1L,plan.paradas())),usuario.getCorreo()));verify(solicitudes,never()).save(any());
    }
    @Test void programacionConsumeElPlanAprobadoSinRecibirloDelCliente(){
        var s=aprobada();var servicio=new ServicioTransporte();servicio.setId(9L);servicio.setCodigo("SER-001");servicio.setRuta(ruta);servicio.setEstado(EstadoServicio.PROGRAMADO);when(servicios.registrar(any())).thenReturn(servicio);
        api.programar(1L,programacion(null));var argumento=ArgumentCaptor.forClass(ServicioRequest.class);verify(servicios).registrar(argumento.capture());
        assertEquals(plan,argumento.getValue().planViaje());assertTrue(argumento.getValue().requiereRetorno());assertEquals(EstadoSolicitud.PROGRAMADA,s.getEstado());
    }
    @Test void impideCambiarElPlanAprobadoAlProgramar(){aprobada();assertThrows(IllegalArgumentException.class,()->api.programar(1L,programacion(new PlanViajeRequest(1L,1L,List.of()))));verifyNoInteractions(servicios);}
    @Test void solicitudesAntiguasDebenCompletarYReaprobarElPlan(){
        var s=aprobada();s.setPlanViaje(null);
        assertThrows(IllegalArgumentException.class,()->api.programar(1L,programacion(plan)));verifyNoInteractions(servicios);
        var resultado=api.completarRecorrido(1L,new RecorridoSolicitudRequest(2L,plan));assertEquals(EstadoSolicitud.EN_REVISION,resultado.estado());assertEquals(plan,resultado.planViaje());
        assertEquals(EstadoSolicitud.APROBADA,api.cambiarEstado(1L,EstadoSolicitud.APROBADA,null).estado());
    }
    @Test void noApruebaSinRecorridoYNoModificaUnPlanAprobado(){
        var s=aprobada();assertThrows(IllegalArgumentException.class,()->api.completarRecorrido(1L,new RecorridoSolicitudRequest(2L,plan)));
        s.setPlanViaje(null);s.setEstado(EstadoSolicitud.EN_REVISION);assertThrows(IllegalArgumentException.class,()->api.cambiarEstado(1L,EstadoSolicitud.APROBADA,null));
    }
    @Test void conservaPlanYGeometriaAlConvertirParaSqlServer(){
        var carretera=new EstimacionTramoRequest(new BigDecimal("40.25"),30,"CARRETERA",List.of(List.of(-8.0,-74.0),List.of(-8.1,-74.1)));
        var original=new PlanViajeRequest(1L,1L,plan.paradas(),List.of(carretera,plan.estimaciones().get(1)));
        var converter=new PlanSolicitudConverter();assertEquals(original,converter.convertToEntityAttribute(converter.convertToDatabaseColumn(original)));assertNull(converter.convertToEntityAttribute(null));
    }
    @Test void destinoPredefinidoPermaneceComoUltimaParada(){
        var acopio=new Ubicacion();acopio.setId(3L);acopio.setNombre("Curimaná");acopio.setTipo(TipoUbicacion.ACOPIO);acopio.setEstado(EstadoRuta.ACTIVA);when(puntos.findById(3L)).thenReturn(Optional.of(acopio));
        var entrega=new PlanViajeRequest.Parada(3L,"ENTREGA","ESCOBAJO",null);
        var estimacion=plan.estimaciones().get(0);
        var correcto=new PlanViajeRequest(1L,1L,List.of(entrega,plan.paradas().get(0)),List.of(estimacion,estimacion,estimacion));
        assertEquals("Tulumayo → Curimaná → Neshuya → Tulumayo",api.registrar(request(correcto),usuario.getCorreo()).resumenRecorrido());
        var incorrecto=new PlanViajeRequest(1L,1L,List.of(plan.paradas().get(0),entrega),correcto.estimaciones());
        assertThrows(IllegalArgumentException.class,()->api.registrar(request(incorrecto),usuario.getCorreo()));
    }

    @Test void noInfiereTipoPlantaPorElNombre(){
        ruta.getOrigenUbicacion().setTipo(TipoUbicacion.OTRO);ruta.getOrigenUbicacion().setNombre("Planta Tulumayo");
        assertThrows(IllegalArgumentException.class,()->api.registrar(request(plan),usuario.getCorreo()));
        assertFalse(ItinerarioViaje.esBase(ruta.getOrigenUbicacion()));
    }

    @Test void rechazaFechaPasadaAntesDeGuardarLaSolicitud(){
        var r=request(plan);var pasada=new SolicitudTransporteRequest(r.tipoSolicitud(),r.solicitanteNombre(),r.areaSolicitante(),r.telefono(),r.origen(),r.destino(),r.productoTransportado(),r.descripcionProducto(),r.cantidadEstimadaToneladas(),LocalDateTime.now().minusMinutes(1),r.prioridad(),r.observaciones(),r.rutaId(),r.planViaje());
        assertThrows(IllegalArgumentException.class,()->api.registrar(pasada,usuario.getCorreo()));verify(solicitudes,never()).save(any());
    }


    @Test void noProgramaAntesDeLaFechaRequeridaAunqueSeaFutura(){
        var s=aprobada();s.setFechaRequerida(LocalDateTime.now().plusDays(3));
        var error=assertThrows(IllegalArgumentException.class,()->api.programar(1L,programacion(null)));
        assertTrue(error.getMessage().contains("fecha y hora requerida"));verifyNoInteractions(servicios);
    }
    @Test void permiteProgramarAlMismoHorarioRequerido(){
        var s=aprobada();var r=programacion(null);s.setFechaRequerida(r.fechaHoraSalida());
        var servicio=new ServicioTransporte();servicio.setId(9L);servicio.setCodigo("SER-001");servicio.setRuta(ruta);servicio.setEstado(EstadoServicio.PROGRAMADO);when(servicios.registrar(any())).thenReturn(servicio);
        api.programar(1L,r);verify(servicios).registrar(any());
    }
}
