package com.palmas.transporte.modulos.identidad.application;

import com.palmas.transporte.modulos.auditoria.domain.Auditoria;
import com.palmas.transporte.modulos.auditoria.persistence.AuditoriaRepository;
import com.palmas.transporte.modulos.identidad.api.CambiarContrasenaRequest;
import com.palmas.transporte.modulos.identidad.api.RestablecerContrasenaRequest;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




class ContrasenaServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AuditoriaRepository auditorias = mock(AuditoriaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ContrasenaService api = new ContrasenaService(usuarios, auditorias, encoder, 5, 15);
    private Usuario actor;
    private Usuario destino;
    private Jwt jwt;

    @BeforeEach
    void preparar() {
        actor = usuario(1L, Rol.ADMINISTRADOR);
        destino = usuario(6L, Rol.CHOFER);
        destino.setDni("75461525");
        when(usuarios.buscarPorIdParaActualizacion(1L)).thenReturn(Optional.of(actor));
        when(usuarios.buscarPorIdParaActualizacion(6L)).thenReturn(Optional.of(destino));
        when(encoder.encode(any(CharSequence.class))).thenReturn("HASH-NUEVO");
        jwt = Jwt.withTokenValue("token").header("alg", "HS256")
            .subject("admin@palmas.com.pe").claim("usuarioId", 1L)
            .claim("versionSeguridad", 3L).build();
    }

    private Usuario usuario(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id); u.setNombres("Prueba"); u.setRol(rol); u.setActivo(true);
        u.setVersionSeguridad(3L); u.setContrasenaHash("HASH-ANTERIOR");
        return u;
    }

    @Test
    void cambioPropioRevocaSesionesYEliminaObligacion() {
        actor.setCambioContrasenaRequerido(true);
        actor.setContrasenaTemporalHasta(LocalDateTime.now().plusHours(1));
        when(encoder.matches("clave-actual", "HASH-ANTERIOR")).thenReturn(true);
        api.cambiar(jwt, new CambiarContrasenaRequest("clave-actual", "Mi nueva frase segura"), null);
        assertEquals("HASH-NUEVO", actor.getContrasenaHash());
        assertEquals(4L, actor.getVersionSeguridad());
        assertFalse(actor.getCambioContrasenaRequerido());
        assertNull(actor.getContrasenaTemporalHasta());
        verify(usuarios).saveAndFlush(actor);
    }

    @Test
    void claveActualIncorrectaNoCambiaHash() {
        var error = assertThrows(ResponseStatusException.class, () ->
            api.cambiar(jwt, new CambiarContrasenaRequest("equivocada", "Mi nueva frase segura"), null));
        assertEquals(400, error.getStatusCode().value());
        assertEquals("HASH-ANTERIOR", actor.getContrasenaHash());
        assertEquals(1, actor.getIntentosFallidos());
        verify(encoder, never()).encode(any());
    }

    @Test
    void quintoIntentoBloqueaYRevoca() {
        actor.setIntentosFallidos(4);
        assertThrows(ResponseStatusException.class, () ->
            api.cambiar(jwt, new CambiarContrasenaRequest("equivocada", "Mi nueva frase segura"), null));
        assertEquals(4L, actor.getVersionSeguridad());
        assertTrue(actor.getBloqueadoHasta().isAfter(LocalDateTime.now()));
    }

    @Test
    void rechazoDeVersionAnterior() {
        actor.setVersionSeguridad(4L);
        var error = assertThrows(ResponseStatusException.class, () ->
            api.cambiar(jwt, new CambiarContrasenaRequest("clave-actual", "Mi nueva frase segura"), null));
        assertEquals(401, error.getStatusCode().value());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void restablecimientoGeneraTemporalYNoRegistraSecretos() {
        var respuesta = api.restablecer(jwt, 6L, new RestablecerContrasenaRequest("Identidad verificada"), null);
        assertEquals(20, respuesta.contrasenaTemporal().length());
        assertEquals("75461525", respuesta.identificador());
        assertTrue(destino.getCambioContrasenaRequerido());
        assertEquals(4L, destino.getVersionSeguridad());
        assertTrue(respuesta.venceEn().isAfter(LocalDateTime.now().plusHours(23)));
        assertEquals("HASH-NUEVO", destino.getContrasenaHash());
        var auditoria = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditorias).save(auditoria.capture());
        assertFalse(auditoria.getValue().getDetalle().contains(respuesta.contrasenaTemporal()));
        assertNull(auditoria.getValue().getValorNuevo());
        assertNull(auditoria.getValue().getValorAnterior());
    }

    @Test
    void noRestableceAdministrador() {
        destino.setRol(Rol.ADMINISTRADOR);
        var error = assertThrows(ResponseStatusException.class, () ->
            api.restablecer(jwt, 6L, new RestablecerContrasenaRequest("Prueba"), null));
        assertEquals(403, error.getStatusCode().value());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void noReactivaCuentaDesactivada() {
        destino.setActivo(false);
        var error = assertThrows(ResponseStatusException.class, () ->
            api.restablecer(jwt, 6L, new RestablecerContrasenaRequest("Prueba"), null));
        assertEquals(409, error.getStatusCode().value());
        assertFalse(destino.getActivo());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void operadorNoPuedeRestablecer() {
        actor.setRol(Rol.OPERADOR);
        var error = assertThrows(ResponseStatusException.class, () ->
            api.restablecer(jwt, 6L, new RestablecerContrasenaRequest("Prueba"), null));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void rechazaTemporalVencida() {
        actor.setCambioContrasenaRequerido(true);
        actor.setContrasenaTemporalHasta(LocalDateTime.now().minusMinutes(1));
        var error = assertThrows(ResponseStatusException.class, () ->
            api.cambiar(jwt, new CambiarContrasenaRequest("clave-actual", "Mi nueva frase segura"), null));
        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void rechazaClaveNuevaIgualAActual() {
        when(encoder.matches("clave-actual", "HASH-ANTERIOR")).thenReturn(true);
        when(encoder.matches("Mi nueva frase segura", "HASH-ANTERIOR")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () ->
            api.cambiar(jwt, new CambiarContrasenaRequest("clave-actual", "Mi nueva frase segura"), null));
        verify(encoder, never()).encode(any());
    }

    @Test
    void rechazaClaveQueExcede72Bytes() {
        assertFalse(new CambiarContrasenaRequest("actual", "漢".repeat(30)).isLongitudSegura());
        assertTrue(new CambiarContrasenaRequest("actual", "Mi nueva frase segura").isLongitudSegura());
    }

    @Test
    void noPermiteRestablecimientoPropio() {
        var error = assertThrows(ResponseStatusException.class, () ->
            api.restablecer(jwt, 1L, new RestablecerContrasenaRequest("Prueba"), null));
        assertEquals(403, error.getStatusCode().value());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void administradorConCambioPendienteNoRestableceOtrasCuentas() {
        actor.setCambioContrasenaRequerido(true);
        var error = assertThrows(ResponseStatusException.class, () ->
            api.restablecer(jwt, 6L, new RestablecerContrasenaRequest("Prueba"), null));
        assertEquals(403, error.getStatusCode().value());
        verify(usuarios, never()).saveAndFlush(any());
    }
}
