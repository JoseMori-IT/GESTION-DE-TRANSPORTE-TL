package com.palmas.transporte.modulos.identidad.infrastructure;


import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

@Configuration
public class InicializadorDatos {

    @Bean
    @ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
    CommandLineRunner crearAdministrador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.password:}") String passwordInicial,
            @Value("${app.bootstrap.correo:}") String correoInicial) {

        return args -> {
            String correo = correoInicial.trim();
            if (correo.isBlank() || !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                throw new IllegalStateException("Configura un correo válido para el administrador inicial.");
            }

            boolean administradorExiste =
                usuarioRepository
                    .existsByCorreoIgnoreCase(
                        correo
                    );

            if (!administradorExiste) {
                if (passwordInicial.isBlank() || passwordInicial.length() < 12
                    || passwordInicial.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                    throw new IllegalStateException("Configura una clave inicial de al menos 12 caracteres y hasta 72 bytes UTF-8.");
                }
                Usuario administrador =
                    new Usuario();
                administrador.setCambioContrasenaRequerido(true);

                administrador.setNombres(
                    "Administrador del sistema"
                );

                administrador.setCorreo(
                    correo
                );

                administrador.setContrasenaHash(
                    passwordEncoder.encode(
                        passwordInicial
                    )
                );

                administrador.setRol(
                    Rol.MASTER
                );

                administrador.setActivo(true);

                administrador.setIntentosFallidos(
                    0
                );

                usuarioRepository.save(
                    administrador
                );

                System.out.println(
                    "Usuario administrador inicial "
                        + "creado correctamente."
                );
            }
        };
    }
}
