package com.palmas.transporte.modulos.flota.application;
import com.palmas.transporte.modulos.operacion.application.MarcacionTramoService;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoOdometroRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.domain.CategoriaLicencia;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import java.util.*;import java.time.*;import java.math.BigDecimal;import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.transaction.annotation.Transactional;
@SpringBootTest @Transactional
class OdometroTransaccionTest {
 @Autowired VehiculoRepository vehiculos;@Autowired ConductorRepository conductores;@Autowired ServicioTransporteRepository viajes;@Autowired MovimientoOdometroRepository historial;@Autowired MarcacionTramoService marcas;@Autowired EntityManager em;
 Vehiculo unidad(){var v=new Vehiculo();String u=UUID.randomUUID().toString().substring(0,8);v.setCodigoInterno(u);v.setPlaca(u);v.setMarca("Prueba");v.setModelo("Camión");v.setAnioFabricacion(2026);v.setTipo(TipoVehiculo.CAMION);v.setCapacidadPasajeros(2);v.setEstado(EstadoVehiculo.DISPONIBLE);return vehiculos.saveAndFlush(v);}
 Conductor chofer(){var c=new Conductor();c.setTipoDocumento(TipoDocumento.OTRO);c.setNumeroDocumento(UUID.randomUUID().toString().substring(0,8));c.setRolTripulacion(RolTripulacion.CHOFER);c.setNombres("Prueba");c.setApellidos("Chofer");c.setTelefono("999999999");c.setEstado(EstadoConductor.DISPONIBLE);c.setNumeroLicencia("LIC-PRUEBA");c.setCategoriaLicencia(CategoriaLicencia.A_IIIC);c.setVencimientoLicencia(LocalDate.now().plusYears(1));return conductores.saveAndFlush(c);}

 ServicioTransporte viaje(){var v=unidad();v.setOdometroKm(new BigDecimal("50000"));v.setEstadoOdometro("OPERATIVO");v.setFechaOdometro(LocalDateTime.now().minusDays(1));var s=new ServicioTransporte();s.setCodigo("ODO-"+UUID.randomUUID().toString().substring(0,8));s.setVehiculo(v);s.setConductor(chofer());s.setEstado(EstadoServicio.PROGRAMADO);s.setFechaHoraSalida(LocalDateTime.now());s.setOrigenDescripcion("Planta");s.setDestinoDescripcion("Planta");s.setProductoTransportado("RFF");s.setDistanciaEstimadaKm(new BigDecimal("30"));for(int n=1;n<=2;n++){var t=new TramoServicio();t.setServicio(s);t.setOrden(n);t.setOrigen(n==1?"Planta":"Destino");t.setDestino(n==1?"Destino":"Planta");t.setActividad(n==1?"RECOJO":"RETORNO");t.setProductoTransportado("RFF");t.setFuenteDistancia("MANUAL");s.getTramos().add(t);}return viajes.saveAndFlush(s);}
 MarcacionServicioRequest evento(int orden,TipoMarcacionServicio tipo,int minutos,String km,boolean falta){return new MarcacionServicioRequest(UUID.randomUUID().toString(),tipo,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,LocalDateTime.now().minusMinutes(10-minutos),true,null,orden,km==null?null:new BigDecimal(km),falta,falta?"Tablero averiado durante el recorrido":null);}
 @Test void recorridoCompletoActualizaBaseYReintentoNoDuplicaHistorial(){var s=viaje();Long id=s.getId(),chofer=s.getConductor().getId(),unidad=s.getVehiculo().getId();var salida=evento(1,TipoMarcacionServicio.SALIDA_IDA,0,"50000",false);marcas.registrar(id,chofer,salida);marcas.registrar(id,chofer,salida);assertEquals(1,historial.findByVehiculoIdOrderByIdDesc(unidad).size());marcas.registrar(id,chofer,evento(1,TipoMarcacionServicio.LLEGADA_DESTINO,2,null,false));marcas.registrar(id,chofer,evento(2,TipoMarcacionServicio.SALIDA_IDA,4,null,false));marcas.registrar(id,chofer,evento(2,TipoMarcacionServicio.LLEGADA_DESTINO,8,"50031",false));em.flush();em.clear();var v=vehiculos.findById(unidad).orElseThrow();assertEquals(0,v.getOdometroKm().compareTo(new BigDecimal("50031")));assertEquals("OPERATIVO",v.getEstadoOdometro());assertEquals(EstadoServicio.COMPLETADO,viajes.findById(id).orElseThrow().getEstado());assertEquals(2,historial.findByVehiculoIdOrderByIdDesc(unidad).size());}
 @Test void averiaAlRegresarConservaBaseYMotivoSinInventarKilometros(){var s=viaje();Long id=s.getId(),c=s.getConductor().getId(),v=s.getVehiculo().getId();marcas.registrar(id,c,evento(1,TipoMarcacionServicio.SALIDA_IDA,0,"50000",false));marcas.registrar(id,c,evento(1,TipoMarcacionServicio.LLEGADA_DESTINO,2,null,false));marcas.registrar(id,c,evento(2,TipoMarcacionServicio.SALIDA_IDA,4,null,false));marcas.registrar(id,c,evento(2,TipoMarcacionServicio.LLEGADA_DESTINO,8,null,true));em.flush();em.clear();var actual=vehiculos.findById(v).orElseThrow();assertEquals(0,actual.getOdometroKm().compareTo(new BigDecimal("50000")));assertEquals("AVERIADO",actual.getEstadoOdometro());var cerrado=viajes.findById(id).orElseThrow();assertEquals(EstadoServicio.COMPLETADO,cerrado.getEstado());assertTrue(cerrado.getTramos().get(1).getOdometroNoDisponibleLlegada());assertNull(cerrado.getTramos().get(1).getOdometroLlegadaKm());assertTrue(cerrado.getTramos().get(1).getMotivoOdometroLlegada().contains("averiado"));}
}
