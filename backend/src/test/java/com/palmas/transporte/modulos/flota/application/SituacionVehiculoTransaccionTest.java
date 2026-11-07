package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.application.SituacionVehiculoService;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoVehiculoRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


import com.palmas.transporte.modulos.flota.api.SituacionVehiculoRequest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
@SpringBootTest
class SituacionVehiculoTransaccionTest {
 @Autowired SituacionVehiculoService servicio;
 @Autowired VehiculoRepository unidades;
 @Autowired MovimientoVehiculoRepository historial;
 @Autowired PlatformTransactionManager transactionManager;
 @MockitoBean ServicioTransporteRepository viajes;
 @MockitoBean MantenimientoRepository mantenimientos;
 Vehiculo nueva(){var v=new Vehiculo();String codigo="TST-"+UUID.randomUUID().toString().substring(0,8);v.setCodigoInterno(codigo);v.setPlaca(codigo);v.setMarca("Prueba");v.setModelo("Modelo");v.setTipo(TipoVehiculo.CAMION);v.setEstado(EstadoVehiculo.DISPONIBLE);v.setAnioFabricacion(2026);v.setCapacidadPasajeros(2);return unidades.saveAndFlush(v);}
 SituacionVehiculoRequest baja(){return new SituacionVehiculoRequest("BAJA",LocalDate.now(),"Retiro definitivo",null,"Acta 001",true);}
 void limpiar(Long id){historial.deleteAll(historial.findByVehiculoIdOrderByIdDesc(id));unidades.deleteById(id);}
 @Test void registraEstadoEHistorialJuntos(){var v=nueva();try{when(viajes.findByVehiculoIdAndEstadoIn(anyLong(),any())).thenReturn(List.of());when(mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(anyLong())).thenReturn(List.of());servicio.registrar(v.getId(),baja(),"COORDINADOR","prueba","Responsable");assertEquals(EstadoVehiculo.BAJA_DEFINITIVA,unidades.findById(v.getId()).orElseThrow().getEstado());assertEquals(1,historial.findByVehiculoIdOrderByIdDesc(v.getId()).size());}finally{limpiar(v.getId());}}
 @Test void bloqueoRevalidaAsignacionConcurrente() throws Exception {
  var v=nueva();var tomado=new CountDownLatch(1);var continuar=new CountDownLatch(1);var asignado=new AtomicBoolean(false);var pool=Executors.newFixedThreadPool(2);
  when(viajes.findByVehiculoIdAndEstadoIn(anyLong(),any())).thenAnswer(i->{if(!asignado.get())return List.of();var s=new ServicioTransporte();s.setCodigo("SER-CONCURRENTE");return List.of(s);});
  when(mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(anyLong())).thenReturn(List.of());
  try{
   var reserva=pool.submit(()->new TransactionTemplate(transactionManager).execute(status->{unidades.bloquearPorId(v.getId()).orElseThrow();tomado.countDown();try{if(!continuar.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}asignado.set(true);return null;}));
   assertTrue(tomado.await(5,TimeUnit.SECONDS));
   var intento=pool.submit(()->servicio.registrar(v.getId(),baja(),"COORDINADOR","prueba","Responsable"));
   continuar.countDown();reserva.get(5,TimeUnit.SECONDS);
   var error=assertThrows(ExecutionException.class,()->intento.get(5,TimeUnit.SECONDS));assertTrue(error.getCause().getMessage().contains("SER-CONCURRENTE"));assertEquals(EstadoVehiculo.DISPONIBLE,unidades.findById(v.getId()).orElseThrow().getEstado());assertTrue(historial.findByVehiculoIdOrderByIdDesc(v.getId()).isEmpty());
  }finally{continuar.countDown();pool.shutdownNow();limpiar(v.getId());}
 }
}
