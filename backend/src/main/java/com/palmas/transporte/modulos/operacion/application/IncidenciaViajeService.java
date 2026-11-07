package com.palmas.transporte.modulos.operacion.application;
import com.palmas.transporte.modulos.flota.application.OdometroService;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoFalla;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.IncidenciaViaje;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;


import com.palmas.transporte.modulos.operacion.api.IncidenciaViajeRequest;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class IncidenciaViajeService {
 private final ServicioTransporteRepository servicios; private final VehiculoRepository vehiculos; private final ConductorRepository conductores; private final MantenimientoRepository mantenimientos; private final UsuarioRepository usuarios; private final EntityManager em;
 private final OdometroService odometros;
 private static final List<EstadoServicio> RESERVAS=List.of(EstadoServicio.PROGRAMADO,EstadoServicio.EN_CURSO);
 public IncidenciaViajeService(ServicioTransporteRepository s,VehiculoRepository v,ConductorRepository c,MantenimientoRepository m,UsuarioRepository u,EntityManager em,OdometroService odometros){servicios=s;vehiculos=v;conductores=c;mantenimientos=m;usuarios=u;this.em=em;this.odometros=odometros;}
 @Transactional public ServicioTransporte gestionar(Long id,IncidenciaViajeRequest r,String rol,String actor,Object conductorId){
  boolean coordinador=Set.of("MASTER","COORDINADOR","ADMINISTRADOR","OPERADOR").contains(rol==null?"":rol);
  if(!coordinador&&!"CHOFER".equals(rol))throw error(HttpStatus.FORBIDDEN,"No tienes permiso para gestionar esta incidencia.");
  ServicioTransporte s=servicios.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Viaje no encontrado."));
  if(!coordinador&&!String.valueOf(s.getConductor().getId()).equals(String.valueOf(conductorId)))throw error(HttpStatus.FORBIDDEN,"El viaje no pertenece al chofer autenticado.");
  em.refresh(s,LockModeType.PESSIMISTIC_WRITE);
  em.refresh(s.getVehiculo(),LockModeType.PESSIMISTIC_WRITE);
  em.refresh(s.getConductor(),LockModeType.PESSIMISTIC_WRITE);
  if(s.getEstado()!=EstadoServicio.EN_CURSO)throw error(HttpStatus.CONFLICT,"La incidencia requiere un viaje en curso. Actualiza el viaje.");
  var activa=s.getIncidencias().stream().filter(IncidenciaViaje::activa).findFirst().orElse(null);
  LocalDateTime ahora=LocalDateTime.now();
  if("REPORTAR".equals(r.accion())){
   if(activa!=null)throw error(HttpStatus.CONFLICT,"Ya hay una avería abierta. Actualiza el viaje.");
   if(s.getServicioAuxiliadoId()!=null)throw error(HttpStatus.CONFLICT,"Un auxilio con avería requiere atención directa del coordinador antes de reasignar el rescate.");
   String descripcion=texto(r.descripcion(),"Describe qué ocurrió."),lugar=texto(r.lugar(),"Indica dónde se encuentra la unidad.");
   if((r.latitud()==null)!=(r.longitud()==null))throw error(HttpStatus.BAD_REQUEST,"La ubicación GPS debe incluir ambas coordenadas.");
   var usuario=usuarios.findByCorreoIgnoreCase(actor).or(()->usuarios.findByDni(actor)).orElseThrow(()->error(HttpStatus.FORBIDDEN,"Usuario no encontrado."));
   if(mantenimientos.existsByVehiculoIdAndEstadoIn(s.getVehiculo().getId(),List.of(EstadoMantenimiento.REPORTADO,EstadoMantenimiento.EN_EVALUACION,EstadoMantenimiento.EN_MANTENIMIENTO,EstadoMantenimiento.REPARADO)))throw error(HttpStatus.CONFLICT,"La unidad ya tiene una atención abierta; el coordinador debe revisarla.");
   var m=new Mantenimiento();m.setCodigo(codigo("MAN"));m.setVehiculo(s.getVehiculo());m.setReportadoPor(usuario);m.setTipoFalla(TipoFalla.OTRO);m.setPrioridad(PrioridadMantenimiento.ALTA);m.setEstado(EstadoMantenimiento.REPORTADO);m.setTitulo("Avería en ruta · "+s.getCodigo());m.setDescripcion(descripcion+". Ubicación: "+lugar);mantenimientos.saveAndFlush(m);
   var i=new IncidenciaViaje();i.setServicio(s);i.setDescripcion(descripcion);i.setLugar(lugar);i.setLatitud(r.latitud());i.setLongitud(r.longitud());i.setEstado("REPORTADA");i.setFechaReporte(ahora);i.setReportadoPor(actor);i.setMantenimientoId(m.getId());i.setHistorial(ahora+" · "+actor+" · REPORTAR");s.getIncidencias().add(i);s.getVehiculo().setEstado(EstadoVehiculo.MANTENIMIENTO);
  }else{
   if(activa==null||!Objects.equals(activa.getId(),r.incidenciaId())||!Objects.equals(activa.getVersionRegistro(),r.version()))throw error(HttpStatus.CONFLICT,"La incidencia cambió. Actualiza antes de continuar.");
   if(Set.of("TECNICO","REPARADA","REMOLQUE","RETORNO_PLANTA").contains(r.accion())&&!coordinador)throw error(HttpStatus.FORBIDDEN,"La atención la asigna el coordinador.");
   switch(r.accion()){
    case "TECNICO" -> {exigir(activa,"REPORTADA");activa.setTecnico(texto(r.tecnico(),"Indica el técnico o proveedor enviado."));activa.setEstado("TECNICO_EN_CAMINO");activa.setFechaAtencion(ahora);activa.setGestionadoPor(actor);var m=mantenimientos.findById(activa.getMantenimientoId()).orElseThrow();m.setEstado(EstadoMantenimiento.EN_EVALUACION);m.setTallerProveedor(activa.getTecnico());m.setDiagnostico("Atención técnica en ruta asignada. "+activa.getDescripcion());mantenimientos.save(m);}
    case "REPARADA" -> {if(!Set.of("REPORTADA","TECNICO_EN_CAMINO").contains(activa.getEstado()))throw error(HttpStatus.CONFLICT,"La avería no está pendiente de reparación en ruta.");if(activa.getFechaAtencion()==null)activa.setFechaAtencion(ahora);if(r.tecnico()!=null&&!r.tecnico().isBlank())activa.setTecnico(r.tecnico().trim());activa.setDiagnostico(texto(r.diagnostico(),"Registra la reparación realizada."));activa.setFechaReparacion(ahora);activa.setEstado("LISTA_CONTINUAR");activa.setGestionadoPor(actor);var m=mantenimientos.findById(activa.getMantenimientoId()).orElseThrow();m.setEstado(EstadoMantenimiento.REPARADO);m.setDiagnostico(activa.getDiagnostico());m.setTrabajoRealizado(activa.getDiagnostico());m.setFechaInicioAtencion(activa.getFechaAtencion());m.setFechaReparacion(ahora);mantenimientos.save(m);}
    case "CONTINUAR" -> {exigir(activa,"LISTA_CONTINUAR");activa.setEstado("RESUELTA");activa.setFechaResolucion(ahora);activa.setResueltoPor(actor);s.getVehiculo().setEstado(EstadoVehiculo.EN_SERVICIO);var m=mantenimientos.findById(activa.getMantenimientoId()).orElseThrow();m.setEstado(EstadoMantenimiento.CERRADO);m.setDiagnostico(activa.getDiagnostico());m.setTrabajoRealizado(activa.getDiagnostico());m.setTallerProveedor(activa.getTecnico());m.setFechaInicioAtencion(activa.getFechaAtencion());m.setFechaReparacion(activa.getFechaReparacion());m.setFechaCierre(ahora);m.setObservacionesCierre("Reparación en ruta; continúa el viaje "+s.getCodigo());mantenimientos.save(m);}
    case "REMOLQUE" -> throw error(HttpStatus.CONFLICT,"El rescate se coordina fuera del sistema. Confirma el retorno a planta cuando la unidad haya llegado.");
    case "RETORNO_PLANTA" -> {
     if(!Set.of("REPORTADA","TECNICO_EN_CAMINO","LISTA_CONTINUAR","REMOLQUE_ASIGNADO").contains(activa.getEstado()))throw error(HttpStatus.CONFLICT,"La avería no está pendiente de retorno.");
     activa.setFechaResolucion(ahora);activa.setResueltoPor(actor);activa.setGestionadoPor(actor);activa.setEstado("RETORNO_CONFIRMADO");
     s.setEstado(EstadoServicio.INTERRUMPIDO);s.setFechaHoraLlegadaReal(ahora);s.getVehiculo().setEstado(EstadoVehiculo.MANTENIMIENTO);s.getConductor().setEstado(EstadoConductor.DISPONIBLE);if(s.getCopiloto()!=null){em.refresh(s.getCopiloto(),LockModeType.PESSIMISTIC_WRITE);s.getCopiloto().setEstado(EstadoConductor.DISPONIBLE);}
     if(r.diagnostico()!=null&&!r.diagnostico().isBlank())activa.setHistorial((activa.getHistorial()==null?"":activa.getHistorial()+"\n")+"Recepción en planta: "+r.diagnostico().trim());
    }
    case "REGRESO" -> {
     exigir(activa,"REMOLQUE_ASIGNADO");var aux=servicios.findById(activa.getServicioAuxilioId()).orElseThrow();em.refresh(aux,LockModeType.PESSIMISTIC_WRITE);if(aux.getEstado()!=EstadoServicio.COMPLETADO)throw error(HttpStatus.CONFLICT,"La unidad de auxilio debe confirmar primero su llegada a planta.");
     var inicial=s.getTramos().isEmpty()?null:s.getTramos().get(0).getOdometroSalidaKm();
     if(r.odometroRegresoKm()!=null&&inicial!=null&&r.odometroRegresoKm().compareTo(inicial)<0)throw error(HttpStatus.BAD_REQUEST,"La lectura final no puede ser menor a la inicial. En remolque puede no aumentar.");
     odometros.registrarRetornoRemolcado(s,r.odometroRegresoKm(),ahora);
     activa.setOdometroRegresoKm(r.odometroRegresoKm());activa.setFechaResolucion(ahora);activa.setResueltoPor(actor);activa.setEstado("RETORNO_CONFIRMADO");s.setEstado(EstadoServicio.INTERRUMPIDO);s.setFechaHoraLlegadaReal(ahora);s.getVehiculo().setEstado(EstadoVehiculo.MANTENIMIENTO);s.getConductor().setEstado(EstadoConductor.DISPONIBLE);if(s.getCopiloto()!=null)s.getCopiloto().setEstado(EstadoConductor.DISPONIBLE);
    }
    default -> throw error(HttpStatus.BAD_REQUEST,"Acción de incidencia no válida.");
   }
  }
  if(activa!=null)activa.setHistorial((activa.getHistorial()==null?"":activa.getHistorial()+"\n")+ahora+" · "+actor+" · "+r.accion());
  return servicios.saveAndFlush(s);
 }

 @Transactional public void completarRetornoAuxilio(ServicioTransporte aux,LocalDateTime llegada,String actor){
  if(aux.getServicioAuxiliadoId()==null)return;
  if(aux.getEstado()!=EstadoServicio.COMPLETADO)throw error(HttpStatus.CONFLICT,"El auxilio debe finalizar primero.");
  var original=servicios.findById(aux.getServicioAuxiliadoId()).orElseThrow(()->error(HttpStatus.CONFLICT,"No se encontró el viaje auxiliado."));
  em.refresh(original,LockModeType.PESSIMISTIC_WRITE);
  var incidencia=original.getIncidencias().stream().filter(i->Objects.equals(i.getServicioAuxilioId(),aux.getId())).findFirst().orElseThrow(()->error(HttpStatus.CONFLICT,"El auxilio no corresponde a la avería."));
  if("RETORNO_CONFIRMADO".equals(incidencia.getEstado()))return;
  if(original.getEstado()!=EstadoServicio.EN_CURSO||!"REMOLQUE_ASIGNADO".equals(incidencia.getEstado()))throw error(HttpStatus.CONFLICT,"La avería cambió. Actualiza antes de finalizar el auxilio.");
  if(llegada.isBefore(incidencia.getFechaReporte()))throw error(HttpStatus.CONFLICT,"La llegada no puede ser anterior al reporte de avería.");
  em.refresh(original.getVehiculo(),LockModeType.PESSIMISTIC_WRITE);em.refresh(original.getConductor(),LockModeType.PESSIMISTIC_WRITE);
  incidencia.setEstado("RETORNO_CONFIRMADO");incidencia.setFechaResolucion(llegada);incidencia.setResueltoPor(actor);
  incidencia.setHistorial((incidencia.getHistorial()==null?"":incidencia.getHistorial()+"\n")+llegada+" · "+actor+" · RETORNO AUTOMÁTICO CON AUXILIO "+aux.getCodigo());
  original.setEstado(EstadoServicio.INTERRUMPIDO);original.setFechaHoraLlegadaReal(llegada);original.getVehiculo().setEstado(EstadoVehiculo.MANTENIMIENTO);original.getConductor().setEstado(EstadoConductor.DISPONIBLE);
  if(original.getCopiloto()!=null){em.refresh(original.getCopiloto(),LockModeType.PESSIMISTIC_WRITE);original.getCopiloto().setEstado(EstadoConductor.DISPONIBLE);}
  servicios.saveAndFlush(original);
 }
 private void exigir(IncidenciaViaje i,String estado){if(!estado.equals(i.getEstado()))throw error(HttpStatus.CONFLICT,"La acción no corresponde al estado de la avería.");}
 private String texto(String texto,String error){if(texto==null||texto.isBlank())throw error(HttpStatus.BAD_REQUEST,error);return texto.trim();}
 private String codigo(String prefijo){return prefijo+"-"+java.time.LocalDate.now().toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
