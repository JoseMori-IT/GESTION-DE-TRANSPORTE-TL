package com.palmas.transporte.modulos.auditoria.domain;
import com.palmas.transporte.modulos.identidad.domain.Usuario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_sistema")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Se guarda el ID como dato simple y no como relación.
     * Así el registro histórico permanece independiente
     * del estado actual del usuario.
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(
        name = "usuario_nombre",
        nullable = false,
        length = 240
    )
    private String usuarioNombre;

    @Column(
        name = "usuario_correo",
        length = 150
    )
    private String usuarioCorreo;

    @Column(
        name = "usuario_rol",
        length = 30
    )
    private String usuarioRol;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(length = 80)
    private String entidad;

    @Column(
        name = "entidad_id",
        length = 80
    )
    private String entidadId;

    @Column(
        name = "codigo_referencia",
        length = 100
    )
    private String codigoReferencia;

    @Column(length = 1000)
    private String detalle;

    @Lob
    @Column(
        name = "valor_anterior",
        columnDefinition = "nvarchar(max)"
    )
    private String valorAnterior;

    @Lob
    @Column(
        name = "valor_nuevo",
        columnDefinition = "nvarchar(max)"
    )
    private String valorNuevo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoAuditoria resultado;

    @Column(
        name = "direccion_ip",
        length = 45
    )
    private String direccionIp;

    @Column(
        name = "user_agent",
        length = 500
    )
    private String userAgent;

    @Column(
        name = "fecha_registro",
        nullable = false
    )
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (resultado == null) {
            resultado = ResultadoAuditoria.EXITOSO;
        }

        if (
            usuarioNombre == null
            || usuarioNombre.isBlank()
        ) {
            usuarioNombre = "Usuario no identificado";
        }
    }
}