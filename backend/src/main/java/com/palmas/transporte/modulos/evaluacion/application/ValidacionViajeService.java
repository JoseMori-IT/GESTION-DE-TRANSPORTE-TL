package com.palmas.transporte.modulos.evaluacion.application;
import com.palmas.transporte.modulos.evaluacion.domain.ValidacionViaje;
import com.palmas.transporte.modulos.evaluacion.persistence.ValidacionViajeRepository;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;


import com.palmas.transporte.modulos.evaluacion.api.ValidacionViajeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class ValidacionViajeService {
 private final ValidacionViajeRepository registros;
 private final ServicioTransporteRepository servicios;
 public ValidacionViajeService(ValidacionViajeRepository registros,ServicioTransporteRepository servicios){this.registros=registros;this.servicios=servicios;}
 @Transactional(readOnly=true) public List<ValidacionViaje> listar(){return registros.findAllByOrderByIdDesc();}
 @Transactional public ValidacionViaje registrar(ValidacionViajeRequest request,String rol,String usuario,String nombre){
  if(!Set.of("MASTER","COORDINADOR","ADMINISTRADOR","OPERADOR").contains(rol == null ? "" : rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo personal operativo autorizado puede validar viajes.");
  var viaje=servicios.bloquearEvidencias(request.servicioId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Viaje no encontrado."));
  if(viaje.getEstado()!=EstadoServicio.COMPLETADO) throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se valida un viaje completado.");
  if(!Set.of("VALIDADO","OBSERVADO").contains(request.estado())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado de validación inválido.");
  String comentario=request.comentario()==null?null:request.comentario().trim();
  if("OBSERVADO".equals(request.estado()) && (comentario==null||comentario.isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica el motivo de la observación.");
  if(comentario!=null && comentario.length()>1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Comentario demasiado largo.");
  var anterior=registros.findFirstByServicioIdOrderByIdDesc(viaje.getId());
  if(!Objects.equals(request.ultimaValidacionId(),anterior.map(ValidacionViaje::getId).orElse(null))) throw new ResponseStatusException(HttpStatus.CONFLICT,"La validación cambió. Actualiza antes de continuar.");
  var v=new ValidacionViaje();v.setServicioId(viaje.getId());v.setEstado(request.estado());v.setComentario(comentario);v.setUsuario(usuario);v.setResponsable(nombre==null||nombre.isBlank()?usuario:nombre);v.setFecha(LocalDateTime.now());
  return registros.save(v);
 }
}
