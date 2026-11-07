package com.palmas.transporte.modulos.flota.application;
import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.MovimientoOdometro;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoOdometroRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;


import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.api.ConsultaServiciosActivos;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OdometroService {
 private final MovimientoOdometroRepository historial;
 private final ConsultaServiciosActivos viajes;
 private final VehiculoRepository vehiculos;
 public OdometroService(MovimientoOdometroRepository h,ConsultaServiciosActivos s,VehiculoRepository v){historial=h;viajes=s;vehiculos=v;}
 public void validarAlta(VehiculoRequest r){validarDatosUnidad(r);}
 private void validarDatosUnidad(VehiculoRequest r){
  if(!Set.of("OPERATIVO","AVERIADO").contains(r.estadoOdometro()==null?"":r.estadoOdometro()))throw error("Indica si el odómetro está operativo o averiado.");
  if("OPERATIVO".equals(r.estadoOdometro())&&r.odometroKm()==null)throw error("Ingresa el odómetro actual de la unidad.");
  if("AVERIADO".equals(r.estadoOdometro())){if(r.odometroKm()!=null)throw error("No ingreses una lectura como válida si el odómetro está averiado.");motivo(r.motivoOdometro());}
  validarNumero(r.odometroKm());
 }
 @Transactional public void registrarUnidad(Vehiculo v,VehiculoRequest r,boolean alta){
  if(!alta&&!Boolean.TRUE.equals(r.actualizarOdometro()))return;
  validarDatosUnidad(r);
  if(!alta){
   var auth=SecurityContextHolder.getContext().getAuthentication();
   if(auth==null||auth.getAuthorities().stream().noneMatch(a->Set.of("ROLE_MASTER","ROLE_COORDINADOR","ROLE_ADMINISTRADOR").contains(a.getAuthority())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el coordinador o administrador puede ajustar el odómetro.");
   if(!Objects.equals(r.versionRegistro(),v.getVersionRegistro()))throw error("La unidad cambió. Actualiza la ficha antes de ajustar el odómetro.");
   if(viajes.unidadEnViaje(v.getId()))throw error("Finaliza el viaje en curso antes de ajustar o reemplazar el odómetro.");
   if(v.getEstado()==EstadoVehiculo.BAJA_DEFINITIVA)throw error("Una unidad con baja definitiva conserva su historial sin nuevos ajustes.");
   motivo(r.motivoOdometro());
  }
  registrar(v,null,alta?"REGISTRO_UNIDAD":"AJUSTE_COORDINADOR",r.odometroKm(),r.estadoOdometro(),r.motivoOdometro(),LocalDateTime.now());
 }
 public void validarMarcacion(ServicioTransporte s,MarcacionServicioRequest r,boolean inicio,boolean fin){
  boolean falta=Boolean.TRUE.equals(r.odometroNoDisponible());
  if(!inicio&&!fin){if(r.odometroKm()!=null||falta||r.motivoOdometro()!=null&&!r.motivoOdometro().isBlank())throw error("El odómetro solo se registra al iniciar y finalizar el viaje.");return;}
  var v=s.getVehiculo();
  if(v.getFechaOdometro()!=null&&r.fechaHoraDispositivo().isBefore(v.getFechaOdometro()))throw error("La marcación es anterior a la última lectura de la unidad. Solicita revisión al coordinador.");
  if(falta){if(r.odometroKm()!=null)throw error("No combines una lectura con la excepción de odómetro averiado.");motivo(r.motivoOdometro());return;}
  if(r.motivoOdometro()!=null&&!r.motivoOdometro().isBlank())throw error("El motivo requiere indicar que el odómetro no está disponible.");
  if("AVERIADO".equals(v.getEstadoOdometro()))throw error("El odómetro figura averiado. Registra la excepción o solicita al coordinador confirmar su reparación.");
  var km=r.odometroKm();if(km==null)throw error("Ingresa el odómetro o indica que está averiado y su motivo.");validarNumero(km);
  var inicial=s.getTramos().stream().min(Comparator.comparing(TramoServicio::getOrden)).map(TramoServicio::getOdometroSalidaKm).orElse(null);
  if(v.getOdometroKm()!=null&&km.compareTo(v.getOdometroKm())<0)throw error("La lectura no puede ser menor que la última válida de la unidad: "+v.getOdometroKm()+" km.");
  if(fin&&inicial!=null&&km.compareTo(inicial)<=0)throw error("El odómetro final debe ser mayor que el de salida.");
  var referencia=inicio?v.getOdometroKm():inicial;
  var plan=plan(s);
  if(referencia!=null&&plan!=null){var diferencia=km.subtract(referencia);if(diferencia.compareTo(plan.multiply(new BigDecimal("3")))>0&&diferencia.subtract(plan).compareTo(new BigDecimal("100"))>0)throw error("La lectura supera 3 veces el recorrido planificado y lo excede en más de 100 km. Revisa el tablero; si el dato es correcto, solicita revisión al coordinador.");}
 }
 @Transactional public void registrarMarcacion(ServicioTransporte s,MarcacionServicioRequest r,boolean inicio,boolean fin){
  if(!inicio&&!fin)return;
  registrar(s.getVehiculo(),s.getId(),inicio?"SALIDA_VIAJE":"REGRESO_VIAJE",r.odometroKm(),Boolean.TRUE.equals(r.odometroNoDisponible())?"AVERIADO":"OPERATIVO",r.motivoOdometro(),r.fechaHoraDispositivo());
 }
 @Transactional public void registrarRetornoRemolcado(ServicioTransporte s,BigDecimal lectura,LocalDateTime fecha){
  if(lectura==null)return;
  validarNumero(lectura);var v=s.getVehiculo();
  if("AVERIADO".equals(v.getEstadoOdometro()))throw error("El odómetro figura averiado. Confirma el retorno sin lectura y conserva la última válida.");
  if(v.getOdometroKm()!=null&&lectura.compareTo(v.getOdometroKm())<0)throw error("El odómetro del retorno no puede ser menor a la última lectura válida de la unidad.");
  if(v.getFechaOdometro()!=null&&fecha.isBefore(v.getFechaOdometro()))throw error("Revisa la fecha del retorno y la última lectura de la unidad.");
  registrar(v,s.getId(),"RETORNO_REMOLCADO",lectura,"OPERATIVO","Retorno por remolque: el odómetro puede no aumentar. No acredita completar el recorrido.",fecha);
 }
 private BigDecimal plan(ServicioTransporte s){
  if(s.getDistanciaEstimadaKm()!=null&&s.getDistanciaEstimadaKm().signum()>0)return s.getDistanciaEstimadaKm();
  if(s.getTramos().isEmpty()||s.getTramos().stream().anyMatch(t->t.getDistanciaEstimadaKm()==null||t.getDistanciaEstimadaKm().signum()<=0))return null;
  return s.getTramos().stream().map(TramoServicio::getDistanciaEstimadaKm).reduce(BigDecimal.ZERO,BigDecimal::add);
 }
 private void registrar(Vehiculo v,Long viaje,String tipo,BigDecimal lectura,String estado,String motivo,LocalDateTime fecha){
  var m=new MovimientoOdometro();m.setVehiculoId(v.getId());m.setServicioId(viaje);m.setTipo(tipo);m.setLecturaAnterior(v.getOdometroKm());m.setLecturaNueva(lectura);m.setEstadoAnterior(v.getEstadoOdometro());m.setEstadoNuevo(estado);m.setMotivo(motivo==null?null:motivo.trim());
  var auth=SecurityContextHolder.getContext().getAuthentication();m.setUsuario(auth==null?"sistema":auth.getName());m.setFechaLectura(fecha);m.setFechaRegistro(LocalDateTime.now());historial.save(m);
  v.setEstadoOdometro(estado);if(lectura!=null){v.setOdometroKm(lectura);v.setFechaOdometro(fecha);}
 }
 @Transactional(readOnly=true) public List<MovimientoOdometro> historial(Long id){if(!vehiculos.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unidad no encontrada.");return historial.findByVehiculoIdOrderByIdDesc(id);}
 private void validarNumero(BigDecimal n){if(n!=null&&(n.signum()<0||n.compareTo(new BigDecimal("99999999.99"))>0||n.stripTrailingZeros().scale()>2))throw error("El odómetro debe estar entre 0 y 99999999.99 km, con hasta dos decimales.");}
 private void motivo(String texto){if(texto==null||texto.trim().length()<5||texto.length()>300)throw error("Describe el motivo del odómetro con entre 5 y 300 caracteres.");}
 private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
