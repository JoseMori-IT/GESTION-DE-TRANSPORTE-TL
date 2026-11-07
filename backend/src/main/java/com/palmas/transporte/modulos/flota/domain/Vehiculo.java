package com.palmas.transporte.modulos.flota.domain;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo {

    @Column(name="odometro_km", precision=10, scale=2)
    private java.math.BigDecimal odometroKm;
    @Column(name="estado_odometro", length=20)
    private String estadoOdometro;
    @Column(name="fecha_odometro")
    private LocalDateTime fechaOdometro;

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * Control de actualizaciones concurrentes.
     */
    @Version
    @Column(
        name = "version_registro",
        nullable = false
    )
    private Long versionRegistro;

    @Column(
        name = "codigo_interno",
        nullable = false,
        unique = true,
        length = 30
    )
    private String codigoInterno;

    @Column(name = "tiene_foto", nullable = false)
    private Boolean tieneFoto = false;

    @Column(
        nullable = false,
        unique = true,
        length = 15
    )
    private String placa;

    @Column(
        nullable = false,
        length = 60
    )
    private String marca;

    @Column(
        nullable = false,
        length = 60
    )
    private String modelo;

    @Column(
        name = "anio_fabricacion",
        nullable = false
    )
    private Integer anioFabricacion;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private TipoVehiculo tipo;

    @Column(name = "capacidad_pasajeros")
    private Integer capacidadPasajeros;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private EstadoVehiculo estado;

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
            estado = EstadoVehiculo.DISPONIBLE;
        }

        if (tieneFoto == null) {
            tieneFoto = false;
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
        codigoInterno =
            limpiarMayusculas(codigoInterno);

        placa =
            limpiarMayusculas(placa);

        marca =
            limpiarTexto(marca);

        modelo =
            limpiarTexto(modelo);
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
