package com.palmas.transporte.modulos.identidad.persistence;


import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.palmas.transporte.modulos.identidad.domain.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    /* La actividad no modifica datos de cuenta ni su versión optimista. */
    @Modifying
    @Query("""
        update Usuario u
        set u.fechaUltimaConexion = :ahora,
            u.ultimaDireccionIp = :ip,
            u.ultimoUserAgent = :agente
        where u.id = :id
          and u.activo = true
          and (u.fechaUltimaConexion is null or u.fechaUltimaConexion <= :limite)
        """)
    int actualizarActividadSiCorresponde(
        @Param("id") Long id,
        @Param("ahora") LocalDateTime ahora,
        @Param("limite") LocalDateTime limite,
        @Param("ip") String ip,
        @Param("agente") String agente
    );

    Optional<Usuario> findByCorreoIgnoreCase(
        String correo
    );

    Optional<Usuario> findByDni(
        String dni
    );

    /*
     * Bloquea el registro durante el proceso de login.
     * Así, dos intentos simultáneos no pueden leer
     * y sobrescribir el mismo contador.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from Usuario u
        where lower(u.correo) = lower(:correo)
        """)
    Optional<Usuario>
        buscarPorCorreoParaAutenticacion(
            @Param("correo")
            String correo
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from Usuario u
        where u.dni = :dni
        """)
    Optional<Usuario>
        buscarPorDniParaAutenticacion(
            @Param("dni")
            String dni
        );

    /*
     * Se utiliza para operaciones sensibles,
     * como revocar todos los tokens del usuario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from Usuario u
        where u.id = :id
        """)
    Optional<Usuario>
        buscarPorIdParaActualizacion(
            @Param("id")
            Long id
        );

    boolean existsByCorreoIgnoreCase(
        String correo
    );

    boolean existsByCorreoIgnoreCaseAndIdNot(
        String correo,
        Long id
    );

    boolean existsByConductorId(
        Long conductorId
    );

    boolean existsByDni(
        String dni
    );

    boolean existsByDniAndIdNot(
        String dni,
        Long id
    );

    List<Usuario>
        findByCargoAdministrativoIsNotNull(
            Sort sort
        );
}
