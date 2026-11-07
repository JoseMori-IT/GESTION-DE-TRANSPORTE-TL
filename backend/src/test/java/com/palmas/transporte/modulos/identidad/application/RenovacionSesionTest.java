package com.palmas.transporte.modulos.identidad.application;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RenovacionSesionTest {
 UsuarioRepository usuarios;JwtService tokens;RenovacionSesionService servicio;Usuario usuario;
 @BeforeEach void preparar(){usuarios=mock(UsuarioRepository.class);tokens=mock(JwtService.class);servicio=new RenovacionSesionService(usuarios,tokens,28800);usuario=new Usuario();usuario.setId(1L);usuario.setNombres("Prueba");usuario.setCorreo("prueba@test.invalid");usuario.setRol(Rol.COORDINADOR);when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));when(tokens.generarToken(any(),any(),any())).thenReturn("renovado");}
 Jwt jwt(Instant inicio){return Jwt.withTokenValue("solo-prueba").header("alg","HS256").subject("prueba@test.invalid").issuedAt(Instant.now().minusSeconds(30)).expiresAt(Instant.now().plusSeconds(60)).claim("usuarioId",1L).claim("versionSeguridad",0L).claim("inicioSesion",inicio.getEpochSecond()).build();}
 @Test void conservaInicioYLimitaLaRenovacion(){var inicio=Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);assertEquals("renovado",servicio.renovar(jwt(inicio)));verify(tokens).generarToken(usuario,inicio,inicio.plusSeconds(28800));}
 @Test void noRenuevaUnaSesionQueSuperoElLimite(){assertThrows(ResponseStatusException.class,()->servicio.renovar(jwt(Instant.now().minusSeconds(28801))));verifyNoInteractions(tokens);}
 @Test void noRenuevaSiLaCuentaFueRevocada(){usuario.setVersionSeguridad(1L);assertThrows(ResponseStatusException.class,()->servicio.renovar(jwt(Instant.now())));verifyNoInteractions(tokens);}
 @Test void noRenuevaCuentasInactivasNiCambioPendiente(){usuario.setActivo(false);assertThrows(ResponseStatusException.class,()->servicio.renovar(jwt(Instant.now())));usuario.setActivo(true);usuario.setCambioContrasenaRequerido(true);assertThrows(ResponseStatusException.class,()->servicio.renovar(jwt(Instant.now())));verifyNoInteractions(tokens);}
 @Test void noRenuevaTokensVencidos(){var vencido=Jwt.withTokenValue("prueba").header("alg","HS256").issuedAt(Instant.now().minusSeconds(120)).expiresAt(Instant.now().minusSeconds(60)).claim("usuarioId",1L).claim("versionSeguridad",0L).build();assertThrows(ResponseStatusException.class,()->servicio.renovar(vencido));verifyNoInteractions(tokens);}
}
