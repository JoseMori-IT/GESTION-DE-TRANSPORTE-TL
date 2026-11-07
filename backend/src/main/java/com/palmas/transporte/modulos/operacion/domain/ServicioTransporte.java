package com.palmas.transporte.modulos.operacion.domain;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;

import java.math.BigDecimal;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "servicios_transporte")
@Getter
@Setter
@NoArgsConstructor
public class ServicioTransporte {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * Permite detectar actualizaciones simultáneas.
     * Hibernate incrementa este valor automáticamente.
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

    @ManyToOne(
        fetch = FetchType.EAGER,
        optional = false
    )
    @JoinColumn(
        name = "vehiculo_id",
        nullable = false
    )
    private Vehiculo vehiculo;

    @ManyToOne(
        fetch = FetchType.EAGER,
        optional = false
    )
    @JoinColumn(
        name = "conductor_id",
        nullable = false
    )
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "copiloto_id")
    private Conductor copiloto;

    @ManyToOne(
        fetch = FetchType.EAGER,
        optional = true
    )
    @JoinColumn(
        name = "ruta_id"
    )
    private Ruta ruta;

    @jakarta.persistence.OneToMany(mappedBy = "servicio", cascade = jakarta.persistence.CascadeType.ALL, fetch = FetchType.EAGER)
    @jakarta.persistence.OrderBy("orden ASC")
    private java.util.List<TramoServicio> tramos = new java.util.ArrayList<>();

    @jakarta.persistence.OneToMany(mappedBy="servicio",cascade=jakarta.persistence.CascadeType.ALL,fetch=FetchType.EAGER)
    @jakarta.persistence.OrderBy("id ASC")
    private java.util.Set<IncidenciaViaje> incidencias = new java.util.LinkedHashSet<>();
    @Column(name="servicio_auxiliado_id") private Long servicioAuxiliadoId;
    @jakarta.persistence.Transient public boolean isDetenidoPorAveria(){return incidencias.stream().anyMatch(IncidenciaViaje::activa);}

    @Column(name = "origen_descripcion", nullable = false, length = 200)
    private String origenDescripcion;

    @Column(name = "destino_descripcion", nullable = false, length = 200)
    private String destinoDescripcion;

    @Column(name = "origen_latitud", precision = 10, scale = 7)
    private BigDecimal origenLatitud;

    @Column(name = "origen_longitud", precision = 10, scale = 7)
    private BigDecimal origenLongitud;

    @Column(name = "destino_latitud", precision = 10, scale = 7)
    private BigDecimal destinoLatitud;

    @Column(name = "destino_longitud", precision = 10, scale = 7)
    private BigDecimal destinoLongitud;

    @Column(name = "distancia_estimada_km", precision = 10, scale = 2)
    private BigDecimal distanciaEstimadaKm;

    @Column(name = "duracion_estimada_minutos")
    private Integer duracionEstimadaMinutos;

    @Column(name = "requiere_retorno", nullable = false)
    private Boolean requiereRetorno = false;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    @Column(name = "fecha_hora_salida_real")
    private LocalDateTime fechaHoraSalidaReal;

    @Column(name = "fecha_hora_llegada_real")
    private LocalDateTime fechaHoraLlegadaReal;

    @Column(
        name = "producto_transportado",
        nullable = false,
        length = 150
    )
    private String productoTransportado;

    @Column(
        name = "descripcion_residuos",
        length = 500
    )
    private String descripcionResiduos;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private EstadoServicio estado;

    @Column(length = 500)
    private String observaciones;

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
            estado = EstadoServicio.PROGRAMADO;
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
        codigo = limpiarMayusculas(codigo);

        productoTransportado =
            limpiarTexto(productoTransportado);

        origenDescripcion = limpiarTexto(origenDescripcion);
        destinoDescripcion = limpiarTexto(destinoDescripcion);

        if (requiereRetorno == null) {
            requiereRetorno = false;
        }

        descripcionResiduos =
            limpiarTexto(descripcionResiduos);

        observaciones =
            limpiarTexto(observaciones);
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
            ? limpio.toUpperCase()
            : null;
    }
}
