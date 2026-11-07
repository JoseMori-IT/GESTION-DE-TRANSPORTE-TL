package com.palmas.transporte.modulos.abastecimientos.application;

import com.palmas.transporte.modulos.abastecimientos.api.AbastecimientoRequest;
import com.palmas.transporte.modulos.abastecimientos.application.AbastecimientoService;
import com.palmas.transporte.modulos.abastecimientos.persistence.AbastecimientoRepository;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;

import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;import jakarta.persistence.EntityManager;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.UUID;
@SpringBootTest @Transactional class AbastecimientoTransaccionTest{
 @Autowired AbastecimientoService api;@Autowired VehiculoRepository unidades;@Autowired AbastecimientoRepository datos;@Autowired EntityManager em;
 @Test void persisteCargaReintentoYAnulacionSinModificarLaUnidad(){
 var v=new Vehiculo();var clave=UUID.randomUUID().toString().substring(0,8);v.setCodigoInterno(clave);v.setPlaca(clave);v.setMarca("Prueba");v.setModelo("Camión");v.setAnioFabricacion(2026);v.setCapacidadPasajeros(2);v.setTipo(TipoVehiculo.CAMION);v.setEstado(EstadoVehiculo.DISPONIBLE);v.setOdometroKm(new BigDecimal("50000"));unidades.saveAndFlush(v);
 var r=new AbastecimientoRequest(v.getId(),LocalDateTime.now().minusMinutes(1).withNano(0),new BigDecimal("35.125"),new BigDecimal("50010"),null,"Grifo","001",null,UUID.randomUUID().toString());var a=api.registrar(r,"prueba");var id=a.getId();assertNotNull(id);assertNotNull(a.getVersionRegistro());assertEquals(id,api.registrar(r,"prueba").getId());assertEquals(1,datos.findByVehiculoIdAndAnuladoFalseOrderByFechaAsc(v.getId()).size());
 api.anular(id,a.getVersionRegistro(),"Corrección de registro","coordinador");em.flush();em.clear();var guardado=datos.findById(id).orElseThrow();assertTrue(guardado.isAnulado());assertEquals(new BigDecimal("35.125"),guardado.getGalones());assertEquals("coordinador",guardado.getAnuladoPor());assertEquals(0,unidades.findById(v.getId()).orElseThrow().getOdometroKm().compareTo(new BigDecimal("50000")));
 }
}
