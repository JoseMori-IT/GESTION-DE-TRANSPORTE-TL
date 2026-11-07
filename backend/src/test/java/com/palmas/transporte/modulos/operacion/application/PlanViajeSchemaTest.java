package com.palmas.transporte.modulos.operacion.application;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PlanViajeSchemaTest {
    @Autowired JdbcTemplate jdbc;
    @Test void permitePlanificarPorUbicacionesSinUnaRutaPorCadaTramo() {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) conexion -> {
            assertTrue(conexion.getMetaData().getURL().startsWith("jdbc:h2:mem:"), "Las pruebas deben usar una base temporal aislada");
            return null;
        });
        Integer admiteRutaOpcional=jdbc.queryForObject("SELECT CASE WHEN IS_NULLABLE='YES' THEN 1 ELSE 0 END FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='TRAMOS_SERVICIO' AND COLUMN_NAME='RUTA_ID'",Integer.class);
        assertEquals(1,admiteRutaOpcional,"Aplicar backend/sql/20261023_planificacion_simple.sql");
        Integer ubicaciones=jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='TRAMOS_SERVICIO' AND COLUMN_NAME IN ('ORIGEN_UBICACION_ID','DESTINO_UBICACION_ID')",Integer.class);
        assertEquals(2,ubicaciones);
        Integer plan=jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='SOLICITUDES_TRANSPORTE' AND COLUMN_NAME IN ('PLAN_VIAJE_JSON','RESUMEN_RECORRIDO','DISTANCIA_ESTIMADA_KM','DURACION_ESTIMADA_MINUTOS')",Integer.class);
        assertEquals(4,plan,"Aplicar backend/sql/20261023_recorrido_solicitud.sql");
        Integer odometros=jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='TRAMOS_SERVICIO' AND COLUMN_NAME IN ('ODOMETRO_SALIDA_KM','ODOMETRO_LLEGADA_KM')",Integer.class);
        assertEquals(2,odometros,"Aplicar backend/sql/20261023_odometro_viaje.sql");
    }
}
