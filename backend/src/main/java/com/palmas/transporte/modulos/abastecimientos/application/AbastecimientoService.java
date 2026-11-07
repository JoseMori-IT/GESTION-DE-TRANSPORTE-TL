package com.palmas.transporte.modulos.abastecimientos.application;
import com.palmas.transporte.modulos.abastecimientos.domain.Abastecimiento;
import com.palmas.transporte.modulos.abastecimientos.persistence.AbastecimientoRepository;

import com.palmas.transporte.modulos.flota.api.UnidadesParaAbastecimiento;
import com.palmas.transporte.modulos.abastecimientos.api.AbastecimientoRequest;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;
@Service public class AbastecimientoService{
 private final AbastecimientoRepository datos;private final UnidadesParaAbastecimiento unidades;
 public AbastecimientoService(AbastecimientoRepository d,UnidadesParaAbastecimiento v){datos=d;unidades=v;}
 @Transactional(readOnly=true) public List<Abastecimiento> listar(){return datos.findAllByOrderByFechaDescIdDesc();}
 @Transactional public Abastecimiento registrar(AbastecimientoRequest r,String actor){
  try{UUID.fromString(r.eventoUuid());}catch(Exception e){throw error("El identificador del registro no es válido.");}
  if(r.fecha()==null||r.fecha().isAfter(LocalDateTime.now()))throw error("La fecha del abastecimiento no puede estar en el futuro.");
  if(r.galones()==null||r.galones().signum()<=0||r.galones().compareTo(new BigDecimal("10000"))>0||r.galones().stripTrailingZeros().scale()>3)throw error("Indica entre 0 y 10000 galones, con hasta tres decimales y mayor que cero.");
  texto(r.grifo(),1,150,"Indica el grifo (hasta 150 caracteres).");
  if(r.odometroKm()==null)texto(r.motivoSinOdometro(),5,300,"Indica por qué no se dispone del odómetro (5 a 300 caracteres).");
  else if(r.odometroKm().signum()<0||r.odometroKm().compareTo(new BigDecimal("99999999.99"))>0||r.odometroKm().stripTrailingZeros().scale()>2)throw error("Revisa la lectura del odómetro.");
  var previo=datos.findByEventoUuid(r.eventoUuid());if(previo.isPresent())return repetido(previo.get(),r,actor);
  var v=unidades.bloquear(r.vehiculoId());
  previo=datos.findByEventoUuid(r.eventoUuid());if(previo.isPresent())return repetido(previo.get(),r,actor);

  if(r.odometroKm()!=null){
   if("AVERIADO".equals(v.estadoOdometro()))throw error("El odómetro figura averiado. Registra sin lectura o actualiza su condición desde Vehículos.");
   if(v.odometroKm()!=null&&v.fechaOdometro()!=null&&!r.fecha().isBefore(v.fechaOdometro())&&r.odometroKm().compareTo(v.odometroKm())<0)throw error("La lectura es menor a la última registrada en la unidad.");
   for(var a:datos.findByVehiculoIdAndAnuladoFalseOrderByFechaAsc(v.id())){
    if(a.getOdometroKm()==null)continue;int cmp=r.odometroKm().compareTo(a.getOdometroKm());
    if((!a.getFecha().isAfter(r.fecha())&&cmp<0)||(!a.getFecha().isBefore(r.fecha())&&cmp>0))throw error("El odómetro no coincide con la secuencia de abastecimientos. Revisa la fecha o la lectura.");
   }
  }
  var a=new Abastecimiento();a.setEventoUuid(r.eventoUuid());a.setVehiculoId(v.id());a.setPlaca(v.placa());a.setFecha(r.fecha());a.setGalones(r.galones());a.setOdometroKm(r.odometroKm());a.setMotivoSinOdometro(r.odometroKm()==null?limpiar(r.motivoSinOdometro()):null);a.setGrifo(r.grifo().trim());a.setComprobante(limpiar(r.comprobante()));a.setObservaciones(limpiar(r.observaciones()));a.setRegistradoPor(actor);a.setFechaRegistro(LocalDateTime.now());return datos.saveAndFlush(a);
 }
 @Transactional public Abastecimiento anular(Long id,Long version,String motivo,String actor){
  var a=datos.findById(id).orElseThrow(()->error("Abastecimiento no encontrado."));
  if(!Objects.equals(a.getVersionRegistro(),version))throw error("El registro cambió. Actualiza antes de anular.");
  if(a.isAnulado())throw error("El registro ya está anulado.");texto(motivo,5,300,"Indica el motivo de anulación (5 a 300 caracteres).");a.setAnulado(true);a.setAnuladoPor(actor);a.setFechaAnulacion(LocalDateTime.now());a.setMotivoAnulacion(motivo.trim());return datos.saveAndFlush(a);
 }
 private Abastecimiento repetido(Abastecimiento a,AbastecimientoRequest r,String actor){
  if(!Objects.equals(a.getRegistradoPor(),actor)||!Objects.equals(a.getVehiculoId(),r.vehiculoId())||!Objects.equals(a.getFecha(),r.fecha())||a.getGalones().compareTo(r.galones())!=0||!Objects.equals(a.getOdometroKm()==null?null:a.getOdometroKm().stripTrailingZeros(),r.odometroKm()==null?null:r.odometroKm().stripTrailingZeros())||!Objects.equals(a.getGrifo(),r.grifo().trim())||!Objects.equals(a.getComprobante(),limpiar(r.comprobante()))||!Objects.equals(a.getObservaciones(),limpiar(r.observaciones()))||!Objects.equals(a.getMotivoSinOdometro(),r.odometroKm()==null?limpiar(r.motivoSinOdometro()):null))throw error("El identificador ya corresponde a otro registro.");return a;
 }
 private static String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
 private static void texto(String s,int min,int max,String mensaje){if(s==null||s.trim().length()<min||s.length()>max)throw error(mensaje);}
 private static ResponseStatusException error(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
}
