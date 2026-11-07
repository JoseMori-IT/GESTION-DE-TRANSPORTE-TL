package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.MovimientoOdometroRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
class OdometroServiceTest {
 final MovimientoOdometroRepository historial=mock(MovimientoOdometroRepository.class);
 final ServicioTransporteRepository viajes=mock(ServicioTransporteRepository.class);
 final OdometroService api=new OdometroService(historial,new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(viajes),mock(VehiculoRepository.class));
 final LocalDateTime hora=LocalDateTime.now().minusHours(1);
 ServicioTransporte s;Vehiculo v;
 @BeforeEach void preparar(){v=new Vehiculo();v.setId(1L);v.setOdometroKm(new BigDecimal("50000"));v.setEstadoOdometro("OPERATIVO");v.setFechaOdometro(hora.minusDays(1));v.setVersionRegistro(2L);s=new ServicioTransporte();s.setId(10L);s.setVehiculo(v);s.setDistanciaEstimadaKm(new BigDecimal("30.74"));var t=new TramoServicio();t.setOrden(1);t.setOdometroSalidaKm(new BigDecimal("50000"));s.getTramos().add(t);}
 @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
 MarcacionServicioRequest r(String km,boolean falta,String motivo){return new MarcacionServicioRequest(UUID.randomUUID().toString(),TipoMarcacionServicio.SALIDA_IDA,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,hora,false,null,1,km==null?null:new BigDecimal(km),falta,motivo);}
 VehiculoRequest unidad(String km,String estado,String motivo){return new VehiculoRequest(null,"ABC-123","Prueba","Modelo",2026,TipoVehiculo.CAMION,2,EstadoVehiculo.DISPONIBLE,km==null?null:new BigDecimal(km),estado,motivo,true,2L);}
 @Test void salidaIgualEsValidaYMenorSeRechaza(){assertDoesNotThrow(()->api.validarMarcacion(s,r("50000",false,null),true,false));assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("49999",false,null),true,false));}
 @Test void cierreIgualOMenorNoEsUnRecorrido(){for(String km:List.of("50000","49999"))assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r(km,false,null),false,true));assertDoesNotThrow(()->api.validarMarcacion(s,r("50031",false,null),false,true));}
 @Test void rechazaOchoMilKmSinActualizarBase(){assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("58000",false,null),false,true));assertEquals(new BigDecimal("50000"),v.getOdometroKm());verifyNoInteractions(historial);}
 @Test void validaPrecisionRangoYObligatoriedad(){for(String km:List.of("-1","100000000","50000.001"))assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r(km,false,null),true,false));assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r(null,false,null),true,false));}
 @Test void averiaExigeMotivoYConservaUltimaLectura(){assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r(null,true," "),true,false));assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("50000",true,"Tablero averiado"),true,false));var r=r(null,true,"Tablero averiado");api.validarMarcacion(s,r,true,false);api.registrarMarcacion(s,r,true,false);assertEquals(new BigDecimal("50000"),v.getOdometroKm());assertEquals(hora.minusDays(1),v.getFechaOdometro());assertEquals("AVERIADO",v.getEstadoOdometro());verify(historial).save(argThat(m->m.getLecturaNueva()==null&&m.getMotivo().equals("Tablero averiado")&&m.getLecturaAnterior().compareTo(new BigDecimal("50000"))==0));}
 @Test void unTableroAveriadoNoSeRehabilitaConLecturaDelChofer(){v.setEstadoOdometro("AVERIADO");assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("50000",false,null),true,false));assertDoesNotThrow(()->api.validarMarcacion(s,r(null,true,"Tablero sin lectura"),true,false));}
 @Test void noSeInventanLecturasEnUnidadesAntiguas(){v.setOdometroKm(null);v.setFechaOdometro(null);assertDoesNotThrow(()->api.validarMarcacion(s,r("50000",false,null),true,false));api.registrarMarcacion(s,r("50000",false,null),true,false);assertEquals(new BigDecimal("50000"),v.getOdometroKm());}
 @Test void rechazaLecturaConFechaAnteriorALaBase(){v.setFechaOdometro(hora.plusMinutes(1));assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("50001",false,null),true,false));}
 @Test void paradasIntermediasNoPidenNiAceptanLectura(){assertDoesNotThrow(()->api.validarMarcacion(s,r(null,false,null),false,false));assertThrows(ResponseStatusException.class,()->api.validarMarcacion(s,r("50001",false,null),false,false));}
 @Test void altaRequiereLecturaOExcepcion(){assertThrows(ResponseStatusException.class,()->api.validarAlta(unidad(null,"OPERATIVO",null)));assertThrows(ResponseStatusException.class,()->api.validarAlta(unidad(null,"AVERIADO",null)));assertDoesNotThrow(()->api.validarAlta(unidad("50000","OPERATIVO",null)));assertDoesNotThrow(()->api.validarAlta(unidad(null,"AVERIADO","Tablero averiado")));}
 void rol(String rol){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("coordinador",null,List.of(new SimpleGrantedAuthority("ROLE_"+rol))));}
 @Test void ajustesRequierenPermisoMotivoYNoViajeEnCurso(){rol("OPERADOR");assertThrows(ResponseStatusException.class,()->api.registrarUnidad(v,unidad("100","OPERATIVO","Cambio de tablero"),false));rol("COORDINADOR");assertThrows(ResponseStatusException.class,()->api.registrarUnidad(v,unidad("100","OPERATIVO",null),false));when(viajes.existsByVehiculoIdAndEstadoIn(any(),any())).thenReturn(true);assertThrows(ResponseStatusException.class,()->api.registrarUnidad(v,unidad("100","OPERATIVO","Cambio de tablero"),false));verifyNoInteractions(historial);}
 @Test void reemplazoConservaLecturaAnteriorYReintentoDesactualizadoNoLaPisa(){rol("COORDINADOR");api.registrarUnidad(v,unidad("100","OPERATIVO","Reemplazo del tablero"),false);verify(historial).save(argThat(m->m.getTipo().equals("AJUSTE_COORDINADOR")&&m.getLecturaAnterior().compareTo(new BigDecimal("50000"))==0&&m.getLecturaNueva().compareTo(new BigDecimal("100"))==0));v.setVersionRegistro(3L);assertThrows(ResponseStatusException.class,()->api.registrarUnidad(v,unidad("200","OPERATIVO","Ajuste anterior"),false));}
 @Test void retornoRemolcadoPermiteIgualYConservaTrazabilidadSinCompletarViaje(){api.registrarRetornoRemolcado(s,new BigDecimal("50000"),hora);verify(historial).save(argThat(m->m.getTipo().equals("RETORNO_REMOLCADO")));assertNull(s.getEstado());assertThrows(ResponseStatusException.class,()->api.registrarRetornoRemolcado(s,new BigDecimal("49999"),hora));}
 @Test void retornoRemolcadoNoInventaLecturaSiNoHayDato(){api.registrarRetornoRemolcado(s,null,hora);verifyNoInteractions(historial);v.setEstadoOdometro("AVERIADO");assertThrows(ResponseStatusException.class,()->api.registrarRetornoRemolcado(s,new BigDecimal("50001"),hora));}
}
