package com.palmas.transporte.modulos.evaluacion.domain;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "encuestas_servicio",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_encuesta_servicio",
            columnNames = "servicio_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class EncuestaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Solo se permite una encuesta por servicio.
     */
    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "servicio_id",
        nullable = false,
        unique = true
    )
    private ServicioTransporte servicio;

    /*
     * Calificaciones del 1 al 5.
     */
    @Column(nullable = false)
    private Integer puntualidad;

    @Column(nullable = false)
    private Integer cumplimiento;

    @Column(nullable = false)
    private Integer atencion;

    @Column(
        name = "satisfaccion_general",
        nullable = false
    )
    private Integer satisfaccionGeneral;

    @Column(length = 1000)
    private String comentarios;

    /*
     * Datos del evaluador almacenados como evidencia
     * histórica, aunque posteriormente cambie su perfil.
     */
    @Column(
        name = "evaluador_nombre",
        nullable = false,
        length = 150
    )
    private String evaluadorNombre;

    @Column(
        name = "evaluador_correo",
        length = 150
    )
    private String evaluadorCorreo;

    @Column(
        name = "tipo_evaluador",
        nullable = false,
        length = 20
    )
    @jakarta.persistence.Enumerated(
        jakarta.persistence.EnumType.STRING
    )
    private TipoEvaluador tipoEvaluador;

    @Column(
        name = "fecha_registro",
        nullable = false
    )
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void asignarFechaRegistro() {
        validarCalificaciones();

        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (tipoEvaluador == null) {
            tipoEvaluador = TipoEvaluador.INTERNO;
        }
    }

    private void validarCalificaciones() {
        validarCalificacion(puntualidad);
        validarCalificacion(cumplimiento);
        validarCalificacion(atencion);
        validarCalificacion(satisfaccionGeneral);
    }

    private void validarCalificacion(Integer valor) {
        if (valor == null || valor < 1 || valor > 5) {
            throw new IllegalArgumentException(
                "Las calificaciones deben estar entre 1 y 5"
            );
        }
    }
}