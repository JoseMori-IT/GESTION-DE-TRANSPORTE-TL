package com.palmas.transporte.modulos.flota.application;

import com.palmas.transporte.modulos.flota.application.OdometroService;
import com.palmas.transporte.modulos.flota.application.VehiculoService;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.FotoVehiculoRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;


import com.palmas.transporte.modulos.flota.api.VehiculoRequest;
import org.springframework.web.server.ResponseStatusException;
class VehiculoEdicionEstadoTest {
 final VehiculoRepository vehiculos=mock(VehiculoRepository.class);
 final ServicioTransporteRepository servicios=mock(ServicioTransporteRepository.class);
 final VehiculoService api=new VehiculoService(vehiculos,new com.palmas.transporte.modulos.operacion.application.ConsultaServiciosActivosService(servicios),mock(FotoVehiculoRepository.class),mock(OdometroService.class));
 VehiculoRequest request(EstadoVehiculo estado){return new VehiculoRequest(null,"ABC-123","Marca","Modelo",2026,TipoVehiculo.CAMION,2,estado);}
 @Test void noConfirmaUnCambioDeEstadoIgnorado(){var v=new Vehiculo();v.setEstado(EstadoVehiculo.INACTIVO);when(vehiculos.bloquearPorId(1L)).thenReturn(Optional.of(v));var e=assertThrows(ResponseStatusException.class,()->api.actualizar(1L,request(EstadoVehiculo.MANTENIMIENTO)));assertEquals(409,e.getStatusCode().value());assertEquals(EstadoVehiculo.INACTIVO,v.getEstado());verify(vehiculos,never()).save(any());}
 @Test void permiteEditarDatosSinCambiarEstado(){var v=new Vehiculo();v.setEstado(EstadoVehiculo.MANTENIMIENTO);when(vehiculos.bloquearPorId(1L)).thenReturn(Optional.of(v));when(vehiculos.save(v)).thenReturn(v);var resultado=api.actualizar(1L,request(EstadoVehiculo.MANTENIMIENTO));assertEquals("Marca",resultado.getMarca());assertEquals(EstadoVehiculo.MANTENIMIENTO,resultado.getEstado());}
 @Test void noCreaUnidadEnMantenimientoSinReporte(){assertThrows(ResponseStatusException.class,()->api.registrar(request(EstadoVehiculo.MANTENIMIENTO)));verify(vehiculos,never()).saveAndFlush(any());}
}
