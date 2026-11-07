package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.operacion.api.EstimacionTramoRequest;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



class DistanciasPlanViajeTest {
    @Test void sumaIdaYRetornoYConservaElTrazadoPorCarretera() {
        var base=new Ubicacion();base.setId(1L);base.setNombre("Tulumayo");base.setTipo(TipoUbicacion.PLANTA);base.setEstado(EstadoRuta.ACTIVA);
        var campo=new Ubicacion();campo.setId(2L);campo.setNombre("Productor");campo.setTipo(TipoUbicacion.PARCELA);campo.setEstado(EstadoRuta.ACTIVA);
        var ruta=new Ruta();ruta.setOrigenUbicacion(base);ruta.setDestinoUbicacion(campo);var servicio=new ServicioTransporte();servicio.setRuta(ruta);
        var ida=new EstimacionTramoRequest(new BigDecimal("40.50"),60,"CARRETERA",List.of(List.of(-8.0,-74.0),List.of(-8.5,-74.5)));
        var regreso=new EstimacionTramoRequest(new BigDecimal("45.25"),70,"MANUAL",null);
        var plan=new PlanViajeRequest(1L,1L,List.of(new PlanViajeRequest.Parada(2L,"ENTREGA","ESCOBAJO",null)),List.of(ida,regreso));
        ItinerarioViaje.prepararPlan(servicio,plan,Map.of(1L,base,2L,campo)::get,List.of());
        assertEquals(new BigDecimal("85.75"),servicio.getDistanciaEstimadaKm());assertEquals(130,servicio.getDuracionEstimadaMinutos());assertEquals("CARRETERA",servicio.getTramos().get(0).getFuenteDistancia());assertNotNull(servicio.getTramos().get(0).getRecorridoJson());assertNull(servicio.getTramos().get(1).getRecorridoJson());
    }
    @Test void rechazaTrazadosConCoordenadasInvalidas() {
        assertFalse(new EstimacionTramoRequest(BigDecimal.TEN,5,"CARRETERA",List.of(List.of(100.0,-74.0),List.of(-8.0,-74.0))).isRecorridoValido());
        assertFalse(new EstimacionTramoRequest(BigDecimal.TEN,5,"CARRETERA",null).isRecorridoValido());
        assertNull(new EstimacionTramoRequest(BigDecimal.TEN,5,"MANUAL",List.of(List.of(-8.0,-74.0),List.of(-9.0,-75.0))).recorridoJson());
    }
}
