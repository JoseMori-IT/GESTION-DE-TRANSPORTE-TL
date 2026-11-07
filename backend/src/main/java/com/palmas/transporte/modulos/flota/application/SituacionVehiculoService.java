package com.palmas.transporte.modulos.flota.application;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.MovimientoVehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoVehiculoRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;


import com.palmas.transporte.modulos.operacion.api.ConsultaServiciosActivos;


import com.palmas.transporte.modulos.flota.api.SituacionVehiculoRequest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class SituacionVehiculoService {
 private final VehiculoRepository vehiculos;
 private final ConsultaServiciosActivos viajes;
 private final MantenimientoRepository mantenimientos;
 private final MovimientoVehiculoRepository historial;
 public SituacionVehiculoService(VehiculoRepository v,ConsultaServiciosActivos s,MantenimientoRepository m,MovimientoVehiculoRepository h){vehiculos=v;viajes=s;mantenimientos=m;historial=h;}
 @Transactional(readOnly=true) public List<MovimientoVehiculo> historial(Long id){if(!vehiculos.existsById(id))throw error(HttpStatus.NOT_FOUND,"Unidad no encontrada.");return historial.findByVehiculoIdOrderByIdDesc(id);}
 @Transactional public MovimientoVehiculo registrar(Long id,SituacionVehiculoRequest r,String rol,String usuario,String nombre){
  if(!Set.of("MASTER","COORDINADOR","ADMINISTRADOR","OPERADOR").contains(rol==null?"":rol))throw error(HttpStatus.FORBIDDEN,"No tienes permiso para gestionar unidades.");
  if("BAJA".equals(r.accion()) && !Set.of("MASTER","COORDINADOR","ADMINISTRADOR").contains(rol))throw error(HttpStatus.FORBIDDEN,"La baja definitiva corresponde al coordinador o administrador.");
  if(r.fechaEfectiva()==null || r.fechaEfectiva().isAfter(LocalDate.now()) || r.motivo()==null || r.motivo().isBlank())throw error(HttpStatus.BAD_REQUEST,"Indica fecha efectiva no futura y motivo.");
  var unidad=vehiculos.bloquearPorId(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Unidad no encontrada."));
  if(unidad.getEstado()==EstadoVehiculo.BAJA_DEFINITIVA)throw error(HttpStatus.CONFLICT,"La unidad tiene baja definitiva y no puede reactivarse.");
  var previo=historial.findFirstByVehiculoIdOrderByIdDesc(id);
  if(previo.isPresent() && r.fechaEfectiva().isBefore(previo.get().getFechaEfectiva()))throw error(HttpStatus.CONFLICT,"La fecha efectiva no puede ser anterior al último movimiento registrado.");
  if(!"BAJA".equals(r.accion()))throw error(HttpStatus.BAD_REQUEST,"Solo se admite la baja definitiva. La disponibilidad se controla mediante viajes y mantenimiento.");
  if(!r.confirmarBaja())throw error(HttpStatus.BAD_REQUEST,"Debes confirmar expresamente la baja definitiva.");
  EstadoVehiculo destino=EstadoVehiculo.BAJA_DEFINITIVA;
  var pendientes=viajes.codigosAsignadosAUnidad(id);
  if(!pendientes.isEmpty())throw error(HttpStatus.CONFLICT,"Resuelve primero los viajes asignados: "+String.join(", ", pendientes));
  var atenciones=mantenimientos.findByVehiculoIdOrderByFechaReporteDesc(id).stream().filter(m->m.getEstado()!=EstadoMantenimiento.CERRADO && m.getEstado()!=EstadoMantenimiento.CANCELADO).toList();
  if(!atenciones.isEmpty())throw error(HttpStatus.CONFLICT,"Cierra primero los mantenimientos abiertos: "+atenciones.stream().map(Mantenimiento::getCodigo).sorted().collect(java.util.stream.Collectors.joining(", ")));
  if(unidad.getEstado()==EstadoVehiculo.EN_SERVICIO || unidad.getEstado()==EstadoVehiculo.MANTENIMIENTO)throw error(HttpStatus.CONFLICT,"La unidad conserva una restricción operativa. Regulariza su viaje o mantenimiento antes de continuar.");
  var registro=new MovimientoVehiculo();registro.setVehiculoId(id);registro.setAccion(r.accion());registro.setEstadoAnterior(unidad.getEstado().name());registro.setEstadoNuevo(destino.name());registro.setFechaEfectiva(r.fechaEfectiva());registro.setMotivo(r.motivo().trim());registro.setObservaciones(limpiar(r.observaciones()));registro.setReferenciaSustento(limpiar(r.referenciaSustento()));registro.setUsuario(usuario);registro.setResponsable(nombre==null||nombre.isBlank()?usuario:nombre);registro.setRegistrado(LocalDateTime.now());
  unidad.setEstado(destino);vehiculos.save(unidad);return historial.save(registro);
 }
 private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
 private ResponseStatusException error(HttpStatus estado,String mensaje){return new ResponseStatusException(estado,mensaje);}
}
