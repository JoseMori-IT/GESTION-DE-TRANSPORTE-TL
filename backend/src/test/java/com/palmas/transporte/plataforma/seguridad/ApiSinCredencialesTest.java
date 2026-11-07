package com.palmas.transporte.plataforma.seguridad;


import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest class ApiSinCredencialesTest {
 @Autowired WebApplicationContext contexto;MockMvc mvc;
 @BeforeEach void preparar(){mvc=webAppContextSetup(contexto).apply(springSecurity()).build();}
 @Test void pedirHtmlNoPermiteLeerUnaApiPrivada()throws Exception{
  for(String ruta:new String[]{"/api/perfil","/api/usuarios","/api/servicios","/api/abastecimientos"}){
   mvc.perform(get(ruta).accept("text/html")).andExpect(status().isUnauthorized());
  }
 }
 @Test void renovacionNoAdmiteAccesoAnonimo()throws Exception{mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/renovar")).andExpect(status().isUnauthorized());}
 @Test void tokenFalsoNoEntregaDatos()throws Exception{
  mvc.perform(get("/api/usuarios").header("Authorization","Bearer token-invalido")).andExpect(status().isUnauthorized());
 }
}
