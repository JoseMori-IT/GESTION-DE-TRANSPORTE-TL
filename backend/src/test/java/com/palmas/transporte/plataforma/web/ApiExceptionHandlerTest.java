package com.palmas.transporte.plataforma.web;



import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void devuelve405ConMetodosPermitidos() {
        var error = new HttpRequestMethodNotSupportedException(
            "DELETE", java.util.List.of("GET", "PUT"));
        var respuesta = handler.manejarMetodoNoPermitido(error);
        assertEquals(405, respuesta.getStatusCode().value());
        assertEquals(java.util.Set.of(HttpMethod.GET, HttpMethod.PUT),
            respuesta.getHeaders().getAllow());
        assertNotNull(respuesta.getBody());
        assertEquals(405, respuesta.getBody().get("estado"));
        assertFalse(respuesta.getBody().containsKey("codigoError"));
    }

    @Test
    void permiteAusenciaDeListaDeMetodos() {
        var respuesta = handler.manejarMetodoNoPermitido(
            new HttpRequestMethodNotSupportedException("DELETE"));
        assertEquals(405, respuesta.getStatusCode().value());
        assertTrue(respuesta.getHeaders().getAllow().isEmpty());
    }
}
