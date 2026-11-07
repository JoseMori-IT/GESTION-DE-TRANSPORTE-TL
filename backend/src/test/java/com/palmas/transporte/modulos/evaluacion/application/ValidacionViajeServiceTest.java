package com.palmas.transporte.modulos.evaluacion.application;

import com.palmas.transporte.modulos.evaluacion.domain.ValidacionViaje;
import com.palmas.transporte.modulos.evaluacion.persistence.ValidacionViajeRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.api.ServicioRequest;
import com.palmas.transporte.modulos.operacion.application.ServicioTransporteService;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import com.palmas.transporte.modulos.evaluacion.api.ValidacionViajeRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
class ValidacionViajeServiceTest {
 final ValidacionViajeRepository registros=mock(ValidacionViajeRepository.class);
 final ServicioTransporteRepository servicios=mock(ServicioTransporteRepository.class);
 final ValidacionViajeService api=new ValidacionViajeService(registros,servicios);
 ServicioTransporte viaje(EstadoServicio estado){var s=new ServicioTransporte();s.setId(1L);s.setEstado(estado);when(servicios.bloquearEvidencias(1L)).thenReturn(Optional.of(s));when(registros.findFirstByServicioIdOrderByIdDesc(1L)).thenReturn(Optional.empty());when(registros.save(any())).thenAnswer(i->i.getArgument(0));return s;}
 @Test void choferYConsultaNoPuedenAprobar(){for(String rol:new String[]{"CHOFER","CONSULTA"}){var e=assertThrows(ResponseStatusException.class,()->api.registrar(new ValidacionViajeRequest(1L,"VALIDADO",null,null),rol,"usuario","Prueba"));assertEquals(403,e.getStatusCode().value());}verifyNoInteractions(servicios,registros);}
 @Test void soloViajesCompletados(){for(var estado:new EstadoServicio[]{EstadoServicio.PROGRAMADO,EstadoServicio.EN_CURSO,EstadoServicio.CANCELADO}){viaje(estado);var e=assertThrows(ResponseStatusException.class,()->api.registrar(new ValidacionViajeRequest(1L,"VALIDADO",null,null),"OPERADOR","operador","Prueba"));assertEquals(409,e.getStatusCode().value());}verify(registros,never()).save(any());}
 @Test void observacionRequiereMotivo(){viaje(EstadoServicio.COMPLETADO);var e=assertThrows(ResponseStatusException.class,()->api.registrar(new ValidacionViajeRequest(1L,"OBSERVADO","  ",null),"OPERADOR","operador","Prueba"));assertEquals(400,e.getStatusCode().value());}
 @Test void registraIdentidadSinModificarElViaje(){var s=viaje(EstadoServicio.COMPLETADO);var v=api.registrar(new ValidacionViajeRequest(1L,"VALIDADO","Revisado",null),"COORDINADOR","cuenta","Responsable");assertEquals("cuenta",v.getUsuario());assertEquals("Responsable",v.getResponsable());assertNotNull(v.getFecha());assertEquals(EstadoServicio.COMPLETADO,s.getEstado());verify(servicios,never()).save(any());}
 @Test void conservaHistorialYRechazaRevisionDesactualizada(){viaje(EstadoServicio.COMPLETADO);var anterior=new ValidacionViaje();anterior.setId(9L);when(registros.findFirstByServicioIdOrderByIdDesc(1L)).thenReturn(Optional.of(anterior));assertThrows(ResponseStatusException.class,()->api.registrar(new ValidacionViajeRequest(1L,"VALIDADO",null,null),"OPERADOR","cuenta","Prueba"));var nuevo=api.registrar(new ValidacionViajeRequest(1L,"OBSERVADO","Falta aclarar espera",9L),"OPERADOR","cuenta","Prueba");assertEquals("OBSERVADO",nuevo.getEstado());verify(registros,never()).delete(any());}
 @Test void nuevoViajeNoAdmiteCopiloto(){var request=mock(com.palmas.transporte.modulos.operacion.api.ServicioRequest.class);when(request.copilotoId()).thenReturn(3L);var apiViaje=new ServicioTransporteService(servicios,mock(VehiculoRepository.class),mock(ConductorRepository.class),mock(RutaRepository.class),mock(jakarta.persistence.EntityManager.class), org.mockito.Mockito.mock(com.palmas.transporte.modulos.configuracion.api.BaseOperativa.class));assertThrows(IllegalArgumentException.class,()->apiViaje.registrar(request));verifyNoInteractions(servicios);}
}
