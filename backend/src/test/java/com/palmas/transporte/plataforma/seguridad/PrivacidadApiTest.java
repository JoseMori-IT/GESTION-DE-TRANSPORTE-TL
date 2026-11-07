package com.palmas.transporte.plataforma.seguridad;

import com.palmas.transporte.modulos.identidad.api.CambiarContrasenaRequest;
import com.palmas.transporte.modulos.identidad.api.LoginRequest;
import com.palmas.transporte.modulos.identidad.api.LoginResponse;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.plataforma.web.ApiPrivacidadFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;



class PrivacidadApiTest {
 private final JsonMapper json = JsonMapper.builder().build();
 @Test void nuncaSerializaHashNiContrasenasDeSolicitudes() {
  var u=new Usuario();u.setNombres("Prueba");u.setContrasenaHash("HASH_PRIVADO");
  assertFalse(json.writeValueAsString(u).contains("HASH_PRIVADO"));
  var login=new LoginRequest("prueba@palmas.com.pe","CLAVE_PRIVADA");
  assertFalse(json.writeValueAsString(login).contains("CLAVE_PRIVADA"));
  assertFalse(login.toString().contains("CLAVE_PRIVADA"));
  var cambio=new CambiarContrasenaRequest("ANTERIOR_PRIVADA","NUEVA_PRIVADA");
  String texto=json.writeValueAsString(cambio);
  assertFalse(texto.contains("ANTERIOR_PRIVADA"));assertFalse(texto.contains("NUEVA_PRIVADA"));
  var recibido=json.readValue("{\"identificador\":\"prueba@palmas.com.pe\",\"contrasena\":\"CLAVE_PRIVADA\"}",LoginRequest.class);
  assertEquals("CLAVE_PRIVADA",recibido.contrasena());
 }
 @Test void tokenSoloPermaneceEnRespuestaDeAutenticacionNoEnToString(){
  var r=new LoginResponse(1L,"Prueba",null,"CHOFER",1L,"TOKEN_PRIVADO","ok",false);
  assertTrue(json.writeValueAsString(r).contains("TOKEN_PRIVADO"));
  assertFalse(r.toString().contains("TOKEN_PRIVADO"));
 }
 @Test void apiNoSeAlmacenaEnCacheInclusoConError()throws Exception {
  var req=new MockHttpServletRequest("GET","/api/perfil");var res=new MockHttpServletResponse();
  new ApiPrivacidadFilter().doFilter(req,res,(a,b)->((jakarta.servlet.http.HttpServletResponse)b).setStatus(401));
  assertEquals(401,res.getStatus());assertEquals("no-store",res.getHeader("Cache-Control"));assertEquals("nosniff",res.getHeader("X-Content-Type-Options"));
 }
}
