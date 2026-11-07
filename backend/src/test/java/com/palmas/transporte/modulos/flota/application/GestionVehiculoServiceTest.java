package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.api.GestionVehiculoRequest;
import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.application.MantenimientoService;
import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoFalla;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import org.springframework.web.server.ResponseStatusException;
class GestionVehiculoServiceTest {
 final VehiculoService vehiculos=mock(VehiculoService.class);
 final MantenimientoService mantenimientos=mock(MantenimientoService.class);
 final GestionVehiculoService api=new GestionVehiculoService(vehiculos,mantenimientos);
 VehiculoRequest datos(EstadoVehiculo estado){return new VehiculoRequest(null,"ABC-123","Prueba","Modelo",2026,TipoVehiculo.CAMION,2,estado);}
 Vehiculo preparar(EstadoVehiculo estado){var v=new Vehiculo();v.setId(7L);v.setEstado(estado);when(vehiculos.bloquearPorId(7L)).thenReturn(v);when(vehiculos.actualizar(eq(7L),any())).thenReturn(v);when(vehiculos.buscarPorId(7L)).thenReturn(v);return v;}
 MantenimientoRequest atencion(Long id){return new MantenimientoRequest(id,TipoFalla.MANTENIMIENTO_PREVENTIVO,PrioridadMantenimiento.ALTA,"Cambio de aceite","Revisión planificada",10000);}
 @Test void registraMantenimientoDesdeEdicion(){preparar(EstadoVehiculo.INACTIVO);var m=atencion(7L);api.guardar(7L,new GestionVehiculoRequest(datos(EstadoVehiculo.INACTIVO),EstadoVehiculo.MANTENIMIENTO,m),"coordinador");verify(mantenimientos).registrar(m,"coordinador");verify(vehiculos,never()).cambiarEstado(anyLong(),any());}
 @Test void rechazaAtencionDeOtraUnidad(){preparar(EstadoVehiculo.DISPONIBLE);assertThrows(ResponseStatusException.class,()->api.guardar(7L,new GestionVehiculoRequest(datos(EstadoVehiculo.DISPONIBLE),EstadoVehiculo.MANTENIMIENTO,atencion(8L)),"coordinador"));verify(vehiculos,never()).actualizar(anyLong(),any());}
 @Test void noLiberaMantenimientoDesdeEdicion(){preparar(EstadoVehiculo.MANTENIMIENTO);assertThrows(ResponseStatusException.class,()->api.guardar(7L,new GestionVehiculoRequest(datos(EstadoVehiculo.MANTENIMIENTO),EstadoVehiculo.DISPONIBLE,null),"coordinador"));verifyNoInteractions(mantenimientos);}
 @Test void rechazaEstadoDesactualizado(){preparar(EstadoVehiculo.EN_SERVICIO);assertThrows(ResponseStatusException.class,()->api.guardar(7L,new GestionVehiculoRequest(datos(EstadoVehiculo.DISPONIBLE),EstadoVehiculo.MANTENIMIENTO,atencion(7L)),"coordinador"));verify(vehiculos,never()).actualizar(anyLong(),any());}
}
