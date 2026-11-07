package com.palmas.transporte.modulos.identidad.application;



import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    @Test
    void listaDniDeCuentaChoferSinVinculoParaReconocerAccesoExistente() {
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new UsuarioService(usuarios, mock(ConductorRepository.class), mock(PasswordEncoder.class));
        var chofer = new Usuario();
        chofer.setId(15L);
        chofer.setNombres("Prueba");
        chofer.setDni("72162260");
        chofer.setRol(Rol.CHOFER);
        chofer.setActivo(true);
        when(usuarios.findAll(any(Sort.class))).thenReturn(List.of(chofer));
        var jwt = Jwt.withTokenValue("prueba").header("alg", "HS256").claim("rol", "ADMINISTRADOR").build();
        var cuentas = servicio.listar(jwt);
        assertEquals(1, cuentas.size());
        assertEquals("72162260", cuentas.get(0).dni());
        assertNull(cuentas.get(0).conductorId());
        verify(usuarios, never()).save(any());
    }
}
