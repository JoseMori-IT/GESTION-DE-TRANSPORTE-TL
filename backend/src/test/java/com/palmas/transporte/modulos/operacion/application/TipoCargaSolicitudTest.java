package com.palmas.transporte.modulos.operacion.application;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte;
class TipoCargaSolicitudTest {
 @Test void recojoFijaElProductoCorrecto(){assertDoesNotThrow(()->SolicitudTransporteService.validarTipoCarga(TipoSolicitudTransporte.RECOJO_RFF,"RFF"));assertDoesNotThrow(()->SolicitudTransporteService.validarTipoCarga(TipoSolicitudTransporte.RECOJO_FS,"FS"));assertThrows(IllegalArgumentException.class,()->SolicitudTransporteService.validarTipoCarga(TipoSolicitudTransporte.RECOJO_RFF,"FS"));assertThrows(IllegalArgumentException.class,()->SolicitudTransporteService.validarTipoCarga(TipoSolicitudTransporte.RECOJO_FS,"CENIZA"));}
 @Test void otrosTiposPermitenCargaManual(){for(var tipo:new TipoSolicitudTransporte[]{TipoSolicitudTransporte.MOVIMIENTO_INTERNO,TipoSolicitudTransporte.DISPOSICION_SUBPRODUCTO,TipoSolicitudTransporte.OTRO})assertDoesNotThrow(()->SolicitudTransporteService.validarTipoCarga(tipo,"FIBRA"));}
}
