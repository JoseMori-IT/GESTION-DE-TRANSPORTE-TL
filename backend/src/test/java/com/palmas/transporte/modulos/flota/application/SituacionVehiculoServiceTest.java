package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.MovimientoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.FotoVehiculoRepository;
import com.palmas.transporte.modulos.flota.persistence.MovimientoVehiculoRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import com.palmas.transporte.modulos.flota.api.SituacionVehiculoRequest;
import java.time.*;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
class SituacionVehiculoServiceTest {
 final VehiculoRepository unidades=mock(VehiculoRepository.class);
 final ServicioTransporteRepository viajes=mock(ServicioTransporteRepository.class);
 final MantenimientoRepository mantenimientos=mock(MantenimientoRepository.class);
 final MovimientoVehiculoRepository historial=mock(MovimientoVehiculoRepository.class);
 final SituacionVehiculoService api=new SituacionVehiculoService(unidades,new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(viajes),mantenimientos,historial);
 Vehiculo unidad;
 @BeforeEach void preparar(){unidad=new Vehiculo();unidad.setId(7L);unidad.setEstado(EstadoVehiculo.DISPONIBLE);when(unidades.bloquearPorId(7L)).thenReturn(Optional.of(unidad));when(viajes.findByVehiculoIdAndEstadoIn(anyLong(),any())).thenReturn(List.of());when(mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(anyLong())).thenReturn(List.of());when(historial.save(any())).thenAnswer(i->i.getArgument(0));when(historial.findFirstByVehiculoIdOrderByIdDesc(7L)).thenReturn(Optional.empty());}
 SituacionVehiculoRequest r(String accion,boolean confirmar){return new SituacionVehiculoRequest(accion,LocalDate.now(),"Motivo de prueba","Observación","Acta 001",confirmar);}
 MovimientoVehiculo registrar(String accion){return api.registrar(7L,r(accion,true),"COORDINADOR","cuenta","Responsable");}
 @Test void rechazaSuspensionYRehabilitacion(){for(String accion:List.of("SUSPENDER","REHABILITAR"))assertThrows(ResponseStatusException.class,()->registrar(accion));verify(historial,never()).save(any());}
 @Test void bajaEsIrreversibleYNoSeRepite(){registrar("BAJA");assertEquals(EstadoVehiculo.BAJA_DEFINITIVA,unidad.getEstado());for(String accion:List.of("BAJA","REHABILITAR","SUSPENDER"))assertThrows(ResponseStatusException.class,()->registrar(accion));verify(historial,times(1)).save(any());}
 @Test void requiereConfirmacionExplicita(){var e=assertThrows(ResponseStatusException.class,()->api.registrar(7L,r("BAJA",false),"COORDINADOR","cuenta","Responsable"));assertEquals(400,e.getStatusCode().value());verify(historial,never()).save(any());}
 @Test void permisosDeBajaYAcceso(){for(String rol:List.of("CHOFER","CONSULTA","OPERADOR")){var e=assertThrows(ResponseStatusException.class,()->api.registrar(7L,r("BAJA",true),rol,"cuenta","Responsable"));assertEquals(403,e.getStatusCode().value());}verify(unidades,never()).save(any());}
 @Test void viajesAsignadosBloqueanYSeIdentifican(){for(var estado:List.of(EstadoServicio.PROGRAMADO,EstadoServicio.EN_CURSO)){var s=new ServicioTransporte();s.setEstado(estado);s.setCodigo("SER-001");when(viajes.findByVehiculoIdAndEstadoIn(anyLong(),any())).thenReturn(List.of(s));var e=assertThrows(ResponseStatusException.class,()->registrar("BAJA"));assertTrue(e.getReason().contains("SER-001"));}verify(historial,never()).save(any());}
 @Test void todasLasEtapasDeMantenimientoAbiertoBloquean(){for(var estado:List.of(EstadoMantenimiento.REPORTADO,EstadoMantenimiento.EN_EVALUACION,EstadoMantenimiento.EN_MANTENIMIENTO,EstadoMantenimiento.REPARADO)){var m=new Mantenimiento();m.setEstado(estado);m.setCodigo("MAN-001");when(mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(7L)).thenReturn(List.of(m));assertTrue(assertThrows(ResponseStatusException.class,()->registrar("BAJA")).getReason().contains("MAN-001"));}verify(historial,never()).save(any());}
 @Test void historialCerradoNoBloquea(){var cerrado=new Mantenimiento();cerrado.setEstado(EstadoMantenimiento.CERRADO);var cancelado=new Mantenimiento();cancelado.setEstado(EstadoMantenimiento.CANCELADO);when(mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(7L)).thenReturn(List.of(cerrado,cancelado));registrar("BAJA");assertEquals(EstadoVehiculo.BAJA_DEFINITIVA,unidad.getEstado());}
 @Test void legadoSoloAdmiteBajaExplicita(){unidad.setEstado(EstadoVehiculo.INACTIVO);var m=registrar("BAJA");assertEquals("INACTIVO",m.getEstadoAnterior());assertEquals(EstadoVehiculo.BAJA_DEFINITIVA,unidad.getEstado());}
 @Test void rechazaFechaFutura(){var r=new SituacionVehiculoRequest("BAJA",LocalDate.now().plusDays(1),"Motivo",null,null,true);assertThrows(ResponseStatusException.class,()->api.registrar(7L,r,"COORDINADOR","cuenta","Responsable"));verify(historial,never()).save(any());}
 @Test void endpointsAnterioresNoReactivanBajas(){unidad.setEstado(EstadoVehiculo.BAJA_DEFINITIVA);var apiUnidad=new VehiculoService(unidades,new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(viajes),mock(FotoVehiculoRepository.class),mock(OdometroService.class));assertThrows(ResponseStatusException.class,()->apiUnidad.cambiarEstado(7L,EstadoVehiculo.DISPONIBLE));verify(unidades,never()).save(any());}
}
