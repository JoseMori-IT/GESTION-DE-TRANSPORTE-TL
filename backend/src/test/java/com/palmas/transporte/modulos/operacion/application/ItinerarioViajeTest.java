package com.palmas.transporte.modulos.operacion.application;

import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.palmas.transporte.modulos.operacion.api.TramoRequest;

class ItinerarioViajeTest {
    private Ruta ruta(long id, long origen, long destino) {
        var r = new Ruta(); r.setId(id); r.setOrigen("Punto " + origen); r.setDestino("Punto " + destino);
        var o = new Ubicacion(); o.setId(origen); var d = new Ubicacion(); d.setId(destino);
        r.setOrigenUbicacion(o); r.setDestinoUbicacion(d); r.setDistanciaKm(new BigDecimal("10.00")); r.setDuracionEstimadaMinutos(30); return r;
    }
    @Test void admiteEntregaRecojoYRetornoManteniendoUnSoloViaje() {
        var a = ruta(1,1,2); var b = ruta(2,2,3); var c = ruta(3,3,1); var catalogo = Map.of(1L,a,2L,b,3L,c);
        var s = new ServicioTransporte(); s.setRuta(a); s.setProductoTransportado("ESCOBAJO");
        ItinerarioViaje.preparar(s,List.of(new TramoRequest(1L,"ESCOBAJO","ENTREGA","Descargar"),new TramoRequest(2L,"SIN_CARGA","RECOJO","Recoger fruto"),new TramoRequest(3L,"RFF","RETORNO","Volver a base")),catalogo::get);
        assertEquals(3,s.getTramos().size()); assertEquals("Punto 1",s.getDestinoDescripcion()); assertEquals(new BigDecimal("30.00"),s.getDistanciaEstimadaKm());
        assertEquals("RFF",s.getTramos().get(2).getProductoTransportado()); assertEquals(3,s.getTramos().get(2).getOrden());
        assertTrue(s.getRequiereRetorno());
    }
    @Test void rechazaTramosDesconectados() {
        var a=ruta(1,1,2); var b=ruta(2,9,3); var s=new ServicioTransporte();s.setRuta(a);s.setProductoTransportado("RFF");
        assertThrows(IllegalArgumentException.class,()->ItinerarioViaje.preparar(s,List.of(new TramoRequest(1L,"RFF","ENTREGA",null),new TramoRequest(2L,"RFF","RECOJO",null)),Map.of(1L,a,2L,b)::get));
    }
    @Test void rechazaPrimerTramoDiferenteDeLaSolicitud() {
        var s=new ServicioTransporte();s.setRuta(ruta(1,1,2));
        assertThrows(IllegalArgumentException.class,()->ItinerarioViaje.preparar(s,List.of(new TramoRequest(2L,"RFF","ENTREGA",null)),id->null));
    }
    @Test void conservaViajeSimpleConRetornoSinAfectarViajesHistoricos() {
        var a=ruta(1,1,2);var s=new ServicioTransporte();s.setRuta(a);s.setProductoTransportado("RFF");s.setRequiereRetorno(true);
        ItinerarioViaje.preparar(s,null,id->a);
        assertEquals(2,s.getTramos().size());assertEquals("RETORNO",s.getTramos().get(1).getActividad());assertEquals("Punto 1",s.getTramos().get(1).getDestino());
    }
}
