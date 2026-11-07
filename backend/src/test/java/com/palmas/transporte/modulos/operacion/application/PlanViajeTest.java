package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



class PlanViajeTest {
    private Ubicacion punto(long id,TipoUbicacion tipo) {var p=new Ubicacion();p.setId(id);p.setTipo(tipo);p.setNombre("Punto "+id);p.setEstado(EstadoRuta.ACTIVA);return p;}
    private ServicioTransporte servicio(Ubicacion a,Ubicacion b){var s=new ServicioTransporte();var r=new Ruta();r.setId(1L);r.setOrigenUbicacion(a);r.setDestinoUbicacion(b);s.setRuta(r);return s;}
    @Test void generaIdaYRetornoSinCrearRutasEnElCatalogo() {
        var planta=punto(1,TipoUbicacion.PLANTA);var campo=punto(2,TipoUbicacion.PARCELA);var s=servicio(campo,planta);
        var plan=new PlanViajeRequest(1L,1L,List.of(new PlanViajeRequest.Parada(2L,"RECOJO","RFF",null)));
        ItinerarioViaje.prepararPlan(s,plan,Map.of(1L,planta,2L,campo)::get,List.of());
        assertEquals(2,s.getTramos().size());assertEquals("Punto 1",s.getTramos().get(0).getOrigen());assertEquals("Punto 2",s.getTramos().get(0).getDestino());assertEquals("RETORNO",s.getTramos().get(1).getActividad());assertEquals("RFF",s.getTramos().get(1).getProductoTransportado());assertTrue(s.getRequiereRetorno());assertNull(s.getDistanciaEstimadaKm());assertNull(s.getTramos().get(0).getRuta());
    }
    @Test void admiteEntregaYRecojoEnPuntosDistintosAntesDelRetorno() {
        var planta=punto(1,TipoUbicacion.PLANTA);var a=punto(2,TipoUbicacion.PARCELA);var b=punto(3,TipoUbicacion.ACOPIO);var s=servicio(planta,b);
        var plan=new PlanViajeRequest(1L,1L,List.of(new PlanViajeRequest.Parada(2L,"ENTREGA","ESCOBAJO",null),new PlanViajeRequest.Parada(3L,"RECOJO","RFF",null)));
        ItinerarioViaje.prepararPlan(s,plan,Map.of(1L,planta,2L,a,3L,b)::get,List.of());
        assertEquals(3,s.getTramos().size());assertEquals("Punto 1",s.getDestinoDescripcion());assertEquals("RFF",s.getTramos().get(2).getProductoTransportado());
    }
    @Test void impideTerminarFueraDePlantaYRepetirPuntosConsecutivos() {
        var planta=punto(1,TipoUbicacion.PLANTA);var campo=punto(2,TipoUbicacion.PARCELA);var puntos=Map.of(1L,planta,2L,campo);
        assertThrows(IllegalArgumentException.class,()->ItinerarioViaje.prepararPlan(servicio(planta,campo),new PlanViajeRequest(1L,2L,List.of(new PlanViajeRequest.Parada(2L,"ENTREGA","RFF",null))),puntos::get,List.of()));
        assertThrows(IllegalArgumentException.class,()->ItinerarioViaje.prepararPlan(servicio(planta,campo),new PlanViajeRequest(1L,1L,List.of(new PlanViajeRequest.Parada(1L,"ENTREGA","RFF",null))),puntos::get,List.of()));
    }
}
