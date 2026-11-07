package com.palmas.transporte.modulos.recorridos.application;

import com.palmas.transporte.modulos.recorridos.application.RutaService;
import com.palmas.transporte.modulos.recorridos.application.UbicacionService;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.palmas.transporte.modulos.recorridos.api.RutaRequest;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;
class RutaCodigoTest {
 @Test void generaCodigoEnElServidorYLoConservaAlEditar(){
  var repo=mock(RutaRepository.class);var puntos=mock(UbicacionService.class);var api=new RutaService(repo,puntos);
  var a=new Ubicacion();a.setNombre("Tulumayo");var b=new Ubicacion();b.setNombre("Destino");when(puntos.buscarActiva(1L)).thenReturn(a);when(puntos.buscarActiva(2L)).thenReturn(b);when(repo.save(any())).thenAnswer(i->i.getArgument(0));
  var r=new RutaRequest("CODIGO-MANUAL","Ruta",1L,2L,BigDecimal.TEN,30,null,EstadoRuta.ACTIVA);
  var ruta=api.registrar(r);assertTrue(ruta.getCodigo().matches("RUT-[0-9A-F]{16}"));String codigo=ruta.getCodigo();
  when(repo.findById(7L)).thenReturn(Optional.of(ruta));api.actualizar(7L,r);assertEquals(codigo,ruta.getCodigo());
 }
}
