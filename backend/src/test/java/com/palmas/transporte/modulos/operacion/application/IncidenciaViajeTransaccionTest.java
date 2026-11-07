package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.domain.CategoriaLicencia;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.IncidenciaViaje;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;


import com.palmas.transporte.modulos.operacion.api.IncidenciaViajeRequest;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
@SpringBootTest @Transactional
class IncidenciaViajeTransaccionTest {
 @Autowired IncidenciaViajeService flujo;@Autowired MarcacionTramoService marcaciones;@Autowired ServicioTransporteRepository viajes;@Autowired VehiculoRepository vehiculos;@Autowired ConductorRepository conductores;@Autowired UsuarioRepository usuarios;@Autowired MantenimientoRepository mantenimientos;@Autowired EntityManager em;
 Vehiculo unidad(){var v=new Vehiculo();String u=UUID.randomUUID().toString().substring(0,8);v.setCodigoInterno(u);v.setPlaca(u);v.setMarca("Prueba");v.setModelo("Camión");v.setAnioFabricacion(2026);v.setTipo(TipoVehiculo.CAMION);v.setCapacidadPasajeros(2);v.setEstado(EstadoVehiculo.DISPONIBLE);return vehiculos.saveAndFlush(v);}
 Conductor chofer(){var c=new Conductor();c.setTipoDocumento(TipoDocumento.OTRO);c.setNumeroDocumento(UUID.randomUUID().toString().substring(0,8));c.setRolTripulacion(RolTripulacion.CHOFER);c.setNombres("Prueba");c.setApellidos("Chofer");c.setTelefono("999999999");c.setEstado(EstadoConductor.DISPONIBLE);c.setNumeroLicencia("LIC-PRUEBA");c.setCategoriaLicencia(CategoriaLicencia.A_IIIC);c.setVencimientoLicencia(LocalDate.now().plusYears(1));return conductores.saveAndFlush(c);}
 ServicioTransporte viaje(){var c=chofer();var v=unidad();var s=new ServicioTransporte();s.setCodigo("TEST-"+UUID.randomUUID().toString().substring(0,8));s.setConductor(c);s.setVehiculo(v);s.setOrigenDescripcion("Tulumayo");s.setDestinoDescripcion("Neshuya");s.setProductoTransportado("RFF");s.setEstado(EstadoServicio.EN_CURSO);s.setFechaHoraSalidaReal(LocalDateTime.now().minusHours(1));s.setFechaHoraSalida(LocalDateTime.now().minusHours(1));c.setEstado(EstadoConductor.EN_SERVICIO);v.setEstado(EstadoVehiculo.EN_SERVICIO);var u=new Usuario();u.setNombres("Prueba");u.setCorreo("inc-"+s.getCodigo()+"@test.invalid");u.setContrasenaHash("no-credencial-de-prueba");u.setRol(Rol.COORDINADOR);usuarios.saveAndFlush(u);return viajes.saveAndFlush(s);}
 String actor(ServicioTransporte s){return "inc-"+s.getCodigo()+"@test.invalid";}
 IncidenciaViajeRequest r(String accion,IncidenciaViaje i,Long v,Long c){return new IncidenciaViajeRequest(accion,"Motor no arranca","Km 12 carretera",null,null,"Técnico prueba","Reparación e instrucciones",v,c,null,i==null?null:i.getId(),i==null?null:i.getVersionRegistro());}
 ServicioTransporte llamar(ServicioTransporte s,String accion,Long v,Long c){var i=s.getIncidencias().stream().filter(IncidenciaViaje::activa).findFirst().orElse(null);return flujo.gestionar(s.getId(),r(accion,i,v,c),"COORDINADOR",actor(s),null);}
 @Test void tecnicoYContinuacionConservanRecorridoYRecursos(){var s=viaje();s=llamar(s,"REPORTAR",null,null);assertTrue(s.isDetenidoPorAveria());assertEquals(EstadoVehiculo.MANTENIMIENTO,s.getVehiculo().getEstado());var i=s.getIncidencias().iterator().next();assertNotNull(i.getMantenimientoId());s=llamar(s,"TECNICO",null,null);s=llamar(s,"REPARADA",null,null);s=llamar(s,"CONTINUAR",null,null);assertFalse(s.isDetenidoPorAveria());assertEquals(EstadoServicio.EN_CURSO,s.getEstado());assertEquals(EstadoVehiculo.EN_SERVICIO,s.getVehiculo().getEstado());assertEquals(EstadoConductor.EN_SERVICIO,s.getConductor().getEstado());assertEquals(EstadoMantenimiento.CERRADO,mantenimientos.findById(i.getMantenimientoId()).orElseThrow().getEstado());assertNotNull(s.getFechaHoraSalidaReal());}

 @Test void remolqueCierraAmbosViajesConTresMarcasSinFingirParadas(){
  var original=viaje();var pendiente=new TramoServicio();pendiente.setServicio(original);pendiente.setOrden(1);pendiente.setOrigen("Tulumayo");pendiente.setDestino("Neshuya");pendiente.setActividad("RECOJO");pendiente.setProductoTransportado("RFF");pendiente.setSalidaReal(LocalDateTime.now().minusMinutes(10));original.getTramos().add(pendiente);viajes.saveAndFlush(original);
  var s=llamar(original,"REPORTAR",null,null);var v=unidad();var c=chofer();var apoyo=new ServicioTransporte();apoyo.setCodigo("AUX-ANTERIOR-"+UUID.randomUUID().toString().substring(0,8));apoyo.setVehiculo(v);apoyo.setConductor(c);apoyo.setOrigenDescripcion("Tulumayo");apoyo.setDestinoDescripcion("Tulumayo");apoyo.setProductoTransportado("AUXILIO");apoyo.setFechaHoraSalida(LocalDateTime.now());apoyo.setEstado(EstadoServicio.PROGRAMADO);apoyo.setServicioAuxiliadoId(s.getId());for(int n=1;n<=2;n++){var t=new TramoServicio();t.setServicio(apoyo);t.setOrden(n);t.setOrigen(n==1?"Tulumayo":"Km 12 carretera");t.setDestino(n==1?"Km 12 carretera":"Tulumayo");t.setActividad(n==1?"AUXILIO":"RETORNO");t.setProductoTransportado("AUXILIO");apoyo.getTramos().add(t);}viajes.saveAndFlush(apoyo);var anterior=s.getIncidencias().iterator().next();anterior.setServicioAuxilioId(apoyo.getId());anterior.setEstado("REMOLQUE_ASIGNADO");viajes.saveAndFlush(s);var i=s.getIncidencias().iterator().next();var aux=viajes.findById(i.getServicioAuxilioId()).orElseThrow();assertEquals(s.getId(),aux.getServicioAuxiliadoId());assertEquals(2,aux.getTramos().size());
  var hora=LocalDateTime.now();com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest ultima=null;
  for(int n=0;n<3;n++){
   int orden=n<2?1:2;var tipo=n==0?TipoMarcacionServicio.SALIDA_IDA:TipoMarcacionServicio.LLEGADA_DESTINO;var lectura=n==0?new java.math.BigDecimal("1000"):n==2?new java.math.BigDecimal("1010"):null;
   ultima=new com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest(UUID.randomUUID().toString(),tipo,new java.math.BigDecimal("-8.4"),new java.math.BigDecimal("-74.8"),java.math.BigDecimal.TEN,hora.plusSeconds(n),false,null,orden,lectura);
   marcaciones.registrar(aux.getId(),c.getId(),ultima);
   if(n==1){assertNotNull(aux.getTramos().get(1).getSalidaReal());assertEquals(EstadoServicio.EN_CURSO,s.getEstado());}
  }
  em.flush();assertEquals(EstadoServicio.COMPLETADO,aux.getEstado());assertEquals(EstadoServicio.INTERRUMPIDO,s.getEstado());assertEquals("RETORNO_CONFIRMADO",i.getEstado());assertEquals(hora.plusSeconds(2),s.getFechaHoraLlegadaReal());assertEquals(EstadoVehiculo.MANTENIMIENTO,s.getVehiculo().getEstado());assertEquals(EstadoConductor.DISPONIBLE,s.getConductor().getEstado());assertNull(s.getTramos().get(0).getLlegadaReal());assertNull(i.getOdometroRegresoKm());assertEquals(EstadoMantenimiento.REPORTADO,mantenimientos.findById(i.getMantenimientoId()).orElseThrow().getEstado());
  var historial=i.getHistorial();marcaciones.registrar(aux.getId(),c.getId(),ultima);assertEquals(historial,i.getHistorial());assertEquals(EstadoVehiculo.DISPONIBLE,aux.getVehiculo().getEstado());assertEquals(EstadoConductor.DISPONIBLE,aux.getConductor().getEstado());
 }
 @Test void bloqueaChoferAjenoDuplicadosYReanudarSinReparacion(){var s=viaje();final var ajeno=s;assertThrows(Exception.class,()->flujo.gestionar(ajeno.getId(),r("REPORTAR",null,null,null),"CHOFER",actor(ajeno),-1L));s=llamar(s,"REPORTAR",null,null);final var detenido=s;assertThrows(Exception.class,()->llamar(detenido,"REPORTAR",null,null));assertThrows(Exception.class,()->llamar(detenido,"CONTINUAR",null,null));}

 @Test void recepcionEnPlantaInterrumpeSinCrearRescateNiCerrarMantenimiento(){
  var s=llamar(viaje(),"REPORTAR",null,null);var i=s.getIncidencias().iterator().next();long cantidad=viajes.count();
  s=llamar(s,"RETORNO_PLANTA",null,null);
  assertEquals(cantidad,viajes.count());assertNull(i.getServicioAuxilioId());assertNull(i.getOdometroRegresoKm());assertEquals("RETORNO_CONFIRMADO",i.getEstado());assertEquals(EstadoServicio.INTERRUMPIDO,s.getEstado());assertEquals(EstadoVehiculo.MANTENIMIENTO,s.getVehiculo().getEstado());assertEquals(EstadoConductor.DISPONIBLE,s.getConductor().getEstado());assertEquals(EstadoMantenimiento.REPORTADO,mantenimientos.findById(i.getMantenimientoId()).orElseThrow().getEstado());assertNotNull(s.getFechaHoraLlegadaReal());assertTrue(i.getHistorial().contains("RETORNO_PLANTA"));
 }
 @Test void permiteReparacionDirectaSinAsignarTecnicoYLuegoContinuar(){
  var s=llamar(viaje(),"REPORTAR",null,null);s=llamar(s,"REPARADA",null,null);assertEquals("LISTA_CONTINUAR",s.getIncidencias().iterator().next().getEstado());s=llamar(s,"CONTINUAR",null,null);assertEquals(EstadoServicio.EN_CURSO,s.getEstado());assertEquals(EstadoVehiculo.EN_SERVICIO,s.getVehiculo().getEstado());
 }
 @Test void choferNoPuedeConfirmarRecepcionNiCrearUnRescate(){
  var s=llamar(viaje(),"REPORTAR",null,null);var i=s.getIncidencias().iterator().next();
  assertThrows(Exception.class,()->flujo.gestionar(s.getId(),r("RETORNO_PLANTA",i,null,null),"CHOFER",actor(s),s.getConductor().getId()));assertEquals(EstadoServicio.EN_CURSO,s.getEstado());
  assertThrows(Exception.class,()->llamar(s,"REMOLQUE",null,null));
 }
}
