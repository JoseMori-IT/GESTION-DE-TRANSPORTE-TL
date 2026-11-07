package com.palmas.transporte.modulos.flota.application;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.mantenimiento.application.MantenimientoService;
import com.palmas.transporte.modulos.flota.api.GestionVehiculoRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class GestionVehiculoService {
 private final VehiculoService vehiculos;
 private final MantenimientoService mantenimientos;
 public GestionVehiculoService(VehiculoService vehiculos,MantenimientoService mantenimientos){this.vehiculos=vehiculos;this.mantenimientos=mantenimientos;}
 @Transactional public Vehiculo guardar(Long id,GestionVehiculoRequest request,String usuario){
  var unidad=vehiculos.bloquearPorId(id);
  if(request.datos().estado()!=unidad.getEstado())throw new ResponseStatusException(HttpStatus.CONFLICT,"El estado cambió. Actualiza la unidad antes de guardar.");
  var destino=request.estadoObjetivo();
  if(destino!=unidad.getEstado() && (destino==EstadoVehiculo.INACTIVO || destino==EstadoVehiculo.SUSPENDIDO || destino==EstadoVehiculo.BAJA_DEFINITIVA || unidad.getEstado()==EstadoVehiculo.SUSPENDIDO || unidad.getEstado()==EstadoVehiculo.BAJA_DEFINITIVA))throw new ResponseStatusException(HttpStatus.CONFLICT,"La baja definitiva se registra desde Dar de baja una unidad. No se permite modificar este estado desde la edición general.");
  if(destino==EstadoVehiculo.EN_SERVICIO && destino!=unidad.getEstado())throw new ResponseStatusException(HttpStatus.CONFLICT,"En operación se asigna al iniciar un viaje.");
  if(unidad.getEstado()==EstadoVehiculo.MANTENIMIENTO && destino!=EstadoVehiculo.MANTENIMIENTO)throw new ResponseStatusException(HttpStatus.CONFLICT,"Primero debes cerrar el mantenimiento de esta unidad.");
  boolean ingresar=destino==EstadoVehiculo.MANTENIMIENTO && unidad.getEstado()!=EstadoVehiculo.MANTENIMIENTO;
  if(ingresar && (request.mantenimiento()==null || !id.equals(request.mantenimiento().vehiculoId())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa los datos de mantenimiento de la unidad seleccionada.");
  if(!ingresar && request.mantenimiento()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La atención de mantenimiento no corresponde al cambio solicitado.");
  var actualizado=vehiculos.actualizar(id,request.datos());
  if(ingresar){mantenimientos.registrar(request.mantenimiento(),usuario);return vehiculos.buscarPorId(id);}
  if(destino!=actualizado.getEstado())return vehiculos.cambiarEstado(id,destino);
  return actualizado;
 }
}
