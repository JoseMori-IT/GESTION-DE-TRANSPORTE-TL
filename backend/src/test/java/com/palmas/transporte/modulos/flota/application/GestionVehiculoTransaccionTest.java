package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.api.GestionVehiculoRequest;
import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import com.palmas.transporte.modulos.flota.application.GestionVehiculoService;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.application.MantenimientoService;
import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoFalla;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
@SpringBootTest
class GestionVehiculoTransaccionTest {
 @Autowired GestionVehiculoService gestion;
 @Autowired VehiculoRepository vehiculos;
 @MockitoBean MantenimientoService mantenimientos;
 @Test void siFallaLaAtencionRevierteLosDatosGenerales(){
  var v=new Vehiculo();v.setCodigoInterno("TEST-ATOMIC");v.setPlaca("TST-ATOMIC");v.setMarca("Original");v.setModelo("Modelo");v.setAnioFabricacion(2026);v.setTipo(TipoVehiculo.CAMION);v.setCapacidadPasajeros(2);v.setEstado(EstadoVehiculo.INACTIVO);var guardado=vehiculos.saveAndFlush(v);
  var datos=new VehiculoRequest(null,v.getPlaca(),"Modificada","Modelo",2026,TipoVehiculo.CAMION,2,EstadoVehiculo.INACTIVO);
  var m=new MantenimientoRequest(guardado.getId(),TipoFalla.MOTOR,PrioridadMantenimiento.ALTA,"Falla","Revisar motor",10000);
  when(mantenimientos.registrar(any(),any())).thenThrow(new IllegalStateException("Prueba de fallo"));
  try{assertThrows(IllegalStateException.class,()->gestion.guardar(guardado.getId(),new GestionVehiculoRequest(datos,EstadoVehiculo.MANTENIMIENTO,m),"prueba"));var actual=vehiculos.findById(guardado.getId()).orElseThrow();assertEquals("Original",actual.getMarca());assertEquals(EstadoVehiculo.INACTIVO,actual.getEstado());}finally{vehiculos.deleteById(guardado.getId());}
 }
}
