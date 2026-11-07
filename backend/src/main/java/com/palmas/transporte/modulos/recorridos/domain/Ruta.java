package com.palmas.transporte.modulos.recorridos.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "origen_ubicacion_id", nullable = false)
    private Ubicacion origenUbicacion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destino_ubicacion_id", nullable = false)
    private Ubicacion destinoUbicacion;

    /* Instantaneas legibles conservadas por compatibilidad. */
    @Column(nullable = false, length = 150)
    private String origen;

    @Column(nullable = false, length = 150)
    private String destino;

    @Column(
        name = "distancia_km",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal distanciaKm;

    @Column(name = "duracion_estimada_minutos", nullable = false)
    private Integer duracionEstimadaMinutos;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void antesDeRegistrar() {
        normalizarDatos();

        if (estado == null) {
            estado = EstadoRuta.ACTIVA;
        }

        fechaRegistro = LocalDateTime.now();
    }

    @PreUpdate
    public void antesDeActualizar() {
        normalizarDatos();
        fechaActualizacion = LocalDateTime.now();
    }

    private void normalizarDatos() {
        if (codigo != null) {
            codigo = codigo.trim().toUpperCase();
        }

        if (nombre != null) {
            nombre = nombre.trim();
        }

        if (origen != null) {
            origen = origen.trim();
        }

        if (destino != null) {
            destino = destino.trim();
        }

        if (observaciones != null) {
            observaciones = observaciones.trim();

            if (observaciones.isBlank()) {
                observaciones = null;
            }
        }
    }
}
