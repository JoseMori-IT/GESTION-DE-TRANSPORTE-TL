package com.palmas.transporte.modulos.operacion.domain;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_transporte")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudTransporte {
    @jakarta.persistence.Convert(converter=PlanSolicitudConverter.class)
    @Column(name="plan_viaje_json",columnDefinition="nvarchar(max)")
    private com.palmas.transporte.modulos.operacion.api.PlanViajeRequest planViaje;
    @Column(name="resumen_recorrido",columnDefinition="nvarchar(max)") private String resumenRecorrido;
    @Column(name="distancia_estimada_km",precision=12,scale=2) private BigDecimal distanciaEstimadaKm;
    @Column(name="duracion_estimada_minutos") private Integer duracionEstimadaMinutos;


    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * Impide que dos usuarios procesen silenciosamente
     * la misma versión de una solicitud.
     */
    @Version
    @Column(
        name = "version_registro",
        nullable = false
    )
    private Long versionRegistro;

    @Column(
        nullable = false,
        unique = true,
        length = 30
    )
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_solicitud",
        nullable = false,
        length = 40
    )
    private TipoSolicitudTransporte tipoSolicitud;

    @Column(
        name = "solicitante_nombre",
        nullable = false,
        length = 150
    )
    private String solicitanteNombre;

    @Column(
        name = "area_solicitante",
        nullable = false,
        length = 100
    )
    private String areaSolicitante;

    @Column(length = 20)
    private String telefono;

    @Column(
        nullable = false,
        length = 200
    )
    private String origen;

    @Column(
        nullable = false,
        length = 200
    )
    private String destino;

    @Column(
        name = "producto_transportado",
        nullable = false,
        length = 100
    )
    private String productoTransportado;

    @Column(
        name = "descripcion_producto",
        length = 500
    )
    private String descripcionProducto;

    @Column(
        name = "cantidad_estimada_toneladas",
        precision = 10,
        scale = 2
    )
    private BigDecimal cantidadEstimadaToneladas;

    @Column(
        name = "fecha_requerida",
        nullable = false
    )
    private LocalDateTime fechaRequerida;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private PrioridadSolicitud prioridad;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private EstadoSolicitud estado;

    @Column(length = 700)
    private String observaciones;

    @Column(
        name = "motivo_rechazo_cancelacion",
        length = 500
    )
    private String motivoRechazoCancelacion;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "usuario_solicitante_id",
        nullable = false
    )
    @JsonIgnore
    private Usuario usuarioSolicitante;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    @JsonIgnore
    private ServicioTransporte servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    @JsonIgnore
    private Ruta ruta;

    @Column(
        name = "fecha_registro",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void antesDeRegistrar() {
        normalizarDatos();

        if (estado == null) {
            estado = EstadoSolicitud.SOLICITADA;
        }

        if (prioridad == null) {
            prioridad = PrioridadSolicitud.NORMAL;
        }

        LocalDateTime ahora =
            LocalDateTime.now();

        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }

        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    public void antesDeActualizar() {
        normalizarDatos();
        fechaActualizacion = LocalDateTime.now();
    }

    private void normalizarDatos() {
        codigo =
            limpiarMayusculas(codigo);

        solicitanteNombre =
            limpiarTexto(solicitanteNombre);

        areaSolicitante =
            limpiarTexto(areaSolicitante);

        telefono =
            limpiarTexto(telefono);

        origen =
            limpiarTexto(origen);

        destino =
            limpiarTexto(destino);

        productoTransportado =
            limpiarMayusculas(
                productoTransportado
            );

        descripcionProducto =
            limpiarTexto(
                descripcionProducto
            );

        observaciones =
            limpiarTexto(observaciones);

        motivoRechazoCancelacion =
            limpiarTexto(
                motivoRechazoCancelacion
            );
    }

    private String limpiarTexto(
            String valor) {

        if (
            valor == null
            || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }

    private String limpiarMayusculas(
            String valor) {

        String limpio = limpiarTexto(valor);

        return limpio != null
            ? limpio.toUpperCase(Locale.ROOT)
            : null;
    }
}