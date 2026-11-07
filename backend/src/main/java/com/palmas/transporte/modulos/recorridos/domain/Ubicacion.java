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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ubicaciones")
@Getter
@Setter
@NoArgsConstructor
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version_registro", nullable = false)
    private Long versionRegistro;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoUbicacion tipo;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(length = 300)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void antesDeRegistrar() {
        normalizar();
        if (estado == null) {
            estado = EstadoRuta.ACTIVA;
        }
        fechaRegistro = LocalDateTime.now();
    }

    @PreUpdate
    public void antesDeActualizar() {
        normalizar();
        fechaActualizacion = LocalDateTime.now();
    }

    private void normalizar() {
        if (codigo != null) {
            codigo = codigo.trim().toUpperCase();
        }
        if (nombre != null) {
            nombre = nombre.trim();
        }
        if (referencia != null) {
            referencia = referencia.trim();
            if (referencia.isBlank()) {
                referencia = null;
            }
        }
    }
}
