package com.palmas.transporte.modulos.configuracion.web;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @Transactional class ConfiguracionPermisosTest {
 @Autowired WebApplicationContext contexto;MockMvc mvc;
 final String ruta="/api/configuracion-operativa";
 final String cuerpo="{\"version\":null,\"sede\":\"Prueba\",\"plantaBaseId\":null,\"galonesPorKm\":0.35,\"kgCo2PorKm\":0.9,\"referenciaCombustible\":\"Prueba\",\"referenciaEmisiones\":\"Prueba\"}";
 @BeforeEach void preparar(){mvc=webAppContextSetup(contexto).apply(springSecurity()).build();}
 @Test void requiereSesion()throws Exception{mvc.perform(get(ruta)).andExpect(status().isUnauthorized());mvc.perform(put(ruta).contentType("application/json").content(cuerpo)).andExpect(status().isUnauthorized());}
 @Test void lecturaAutenticadaNoDaPermisoDeEditar()throws Exception{for(String rol:new String[]{"CHOFER","CONSULTA","OPERADOR"}){var identidad=jwt().authorities(new SimpleGrantedAuthority("ROLE_"+rol));mvc.perform(get(ruta).with(identidad)).andExpect(status().isOk());mvc.perform(put(ruta).with(identidad).contentType("application/json").content(cuerpo)).andExpect(status().isForbidden());}}
 @Test void coordinadorPuedeGuardarYNoSeAceptanFactoresInvalidos()throws Exception{var identidad=jwt().jwt(j->j.subject("coordinador@test.invalid")).authorities(new SimpleGrantedAuthority("ROLE_COORDINADOR"));mvc.perform(put(ruta).with(identidad).contentType("application/json").content(cuerpo.replace("0.35","-1"))).andExpect(status().isBadRequest());mvc.perform(put(ruta).with(identidad).contentType("application/json").content(cuerpo)).andExpect(status().isOk()).andExpect(jsonPath("$.actualizadoPor").value("coordinador@test.invalid"));}
}
