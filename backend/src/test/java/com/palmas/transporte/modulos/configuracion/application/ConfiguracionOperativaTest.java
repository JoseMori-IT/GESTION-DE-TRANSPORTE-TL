package com.palmas.transporte.modulos.configuracion.application;

import com.palmas.transporte.modulos.configuracion.api.ConfiguracionOperativaRequest;
import com.palmas.transporte.modulos.configuracion.persistence.ConfiguracionOperativaRepository;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class ConfiguracionOperativaTest {
 @Autowired ConfiguracionOperativaService servicio;
 @Autowired ConfiguracionOperativaRepository datos;
 @Autowired UbicacionRepository puntos;
 @Autowired EntityManager em;
 @Autowired Validator validator;
 ConfiguracionOperativaRequest solicitud(Long version,Long planta){return new ConfiguracionOperativaRequest(version," Sede de prueba ",planta,new BigDecimal("0.350000"),new BigDecimal("0.900000")," Fuente consumo "," Fuente emisiones ");}
 Ubicacion planta(){var u=new Ubicacion();u.setCodigo(UUID.randomUUID().toString().substring(0,20));u.setNombre("Planta "+u.getCodigo());u.setTipo(TipoUbicacion.PLANTA);u.setEstado(EstadoRuta.ACTIVA);return puntos.saveAndFlush(u);}
 @Test void consultarInicialNoEscribeConfiguracion(){assertEquals(0,datos.count());var c=servicio.consultar();assertNull(c.version());assertNull(c.plantaBaseId());assertEquals(0,c.galonesPorKm().compareTo(BigDecimal.ONE));assertEquals(0,datos.count());}
 @Test void guardaFactoresIndependientesYAutor(){var c=servicio.guardar(solicitud(null,null),"coordinador@test.invalid");em.clear();var d=servicio.consultar();assertEquals("Sede de prueba",d.sede());assertNotNull(c.version());assertEquals("coordinador@test.invalid",d.actualizadoPor());assertNotNull(d.actualizadoEn());assertEquals(0,d.galonesPorKm().compareTo(new BigDecimal("0.35")));assertEquals(0,d.kgCo2PorKm().compareTo(new BigDecimal("0.9")));}
 @Test void noSobrescribeUnaVersionAnterior(){var c=servicio.guardar(solicitud(null,null),"a");var nuevo=servicio.guardar(solicitud(c.version(),null),"b");assertTrue(nuevo.version()>c.version());assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.guardar(solicitud(c.version(),null),"c")).getStatusCode().value());}
 @Test void validaSalidaYRetornoContraPlantaConfigurada(){var p=planta();servicio.guardar(solicitud(null,p.getId()),"a");assertDoesNotThrow(()->servicio.validarPlanta(p.getId(),p.getId()));assertThrows(ResponseStatusException.class,()->servicio.validarPlanta(p.getId()+1,p.getId()));assertThrows(ResponseStatusException.class,()->servicio.validarPlanta(p.getId(),p.getId()+1));}
 @Test void rechazaPuntoQueNoEsPlantaActiva(){var p=planta();p.setTipo(TipoUbicacion.OTRO);puntos.saveAndFlush(p);assertThrows(ResponseStatusException.class,()->servicio.guardar(solicitud(null,p.getId()),"a"));assertEquals(0,datos.count());}
 @Test void plantaInactivaImpideNuevosViajes(){var p=planta();servicio.guardar(solicitud(null,p.getId()),"a");p.setEstado(EstadoRuta.INACTIVA);puntos.saveAndFlush(p);assertThrows(ResponseStatusException.class,()->servicio.validarPlanta(p.getId(),p.getId()));}
 @Test void validaEscalaLimitesYCamposObligatorios(){for(String valor:new String[]{"0","-1","101","0.1234567"}){var v=solicitud(null,null);var invalido=new ConfiguracionOperativaRequest(v.version(),v.sede(),null,new BigDecimal(valor),v.kgCo2PorKm(),v.referenciaCombustible(),v.referenciaEmisiones());assertFalse(validator.validate(invalido).isEmpty(),valor);}var v=solicitud(null,null);assertFalse(validator.validate(new ConfiguracionOperativaRequest(null," ",null,v.galonesPorKm(),new BigDecimal("-1")," "," ")).isEmpty());assertTrue(validator.validate(v).isEmpty());}
}
