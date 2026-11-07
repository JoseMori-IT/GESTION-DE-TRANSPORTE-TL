package com.palmas.transporte.modulos.identidad.domain;


import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        nullable = false,
        length = 120
    )
    private String nombres;

    @Column(length = 120)
    private String apellidos;

    @Column(length = 8)
    private String dni;

    /*
     * El correo es obligatorio para el personal
     * administrativo, pero opcional para los choferes.
     */
    @Column(
        unique = true,
        length = 150
    )
    private String correo;

    @Column(
        name = "contrasena_hash",
        nullable = false,
        length = 255
    )
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "cargo_administrativo",
        length = 40
    )
    private CargoAdministrativo
        cargoAdministrativo;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "fecha_cese")
    private LocalDate fechaCese;

    @Column(
        name = "motivo_desactivacion",
        length = 500
    )
    private String motivoDesactivacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "conductor_id",
        unique = true
    )
    private Conductor conductor;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(
        name = "intentos_fallidos",
        nullable = false
    )
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    /*
     * Se incrementa cuando se requiere revocar
     * los tokens emitidos anteriormente.
     */
    @Column(
        name = "version_seguridad",
        nullable = false
    )
    private Long versionSeguridad = 0L;

    /*
     * Control de concurrencia optimista.
     */
    @Version
    @Column(
        name = "version_registro",
        nullable = false
    )
    private Long versionRegistro = 0L;

    @Column(name = "cambio_contrasena_requerido", nullable = false)
    private Boolean cambioContrasenaRequerido = false;

    @Column(name = "contrasena_temporal_hasta")
    private LocalDateTime contrasenaTemporalHasta;

    @Column(
        name = "fecha_creacion",
        nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_primer_login")
    private LocalDateTime fechaPrimerLogin;

    @Column(name = "fecha_ultimo_login")
    private LocalDateTime fechaUltimoLogin;

    @Column(name = "fecha_ultima_conexion")
    private LocalDateTime fechaUltimaConexion;

    @Column(
        name = "ultima_direccion_ip",
        length = 45
    )
    private String ultimaDireccionIp;

    @Column(
        name = "ultimo_user_agent",
        length = 500
    )
    private String ultimoUserAgent;

    @PrePersist
    public void asignarFechasIniciales() {
        LocalDateTime ahora =
            LocalDateTime.now();

        normalizarDatos();

        if (activo == null) {
            activo = true;
        }

        if (intentosFallidos == null) {
            intentosFallidos = 0;
        }

        if (versionSeguridad == null) {
            versionSeguridad = 0L;
        }

        if (versionRegistro == null) {
            versionRegistro = 0L;
        }

        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }

        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    public void actualizarFecha() {
        normalizarDatos();

        fechaActualizacion =
            LocalDateTime.now();
    }

    private void normalizarDatos() {
        if (nombres != null) {
            nombres = nombres.trim();
        }

        if (apellidos != null) {
            apellidos = apellidos.trim();

            if (apellidos.isBlank()) {
                apellidos = null;
            }
        }

        if (dni != null) {
            dni = dni.trim();

            if (dni.isBlank()) {
                dni = null;
            }
        }

        if (correo != null) {
            correo = correo
                .trim()
                .toLowerCase();

            if (correo.isBlank()) {
                correo = null;
            }
        }

        if (motivoDesactivacion != null) {
            motivoDesactivacion =
                motivoDesactivacion.trim();

            if (motivoDesactivacion.isBlank()) {
                motivoDesactivacion = null;
            }
        }

        if (ultimaDireccionIp != null) {
            ultimaDireccionIp =
                ultimaDireccionIp.trim();

            if (ultimaDireccionIp.isBlank()) {
                ultimaDireccionIp = null;
            }
        }

        if (ultimoUserAgent != null) {
            ultimoUserAgent =
                ultimoUserAgent.trim();

            if (ultimoUserAgent.isBlank()) {
                ultimoUserAgent = null;
            }
        }
    }
}
