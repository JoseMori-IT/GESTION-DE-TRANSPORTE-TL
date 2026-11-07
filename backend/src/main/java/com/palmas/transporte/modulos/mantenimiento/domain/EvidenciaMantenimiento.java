package com.palmas.transporte.modulos.mantenimiento.domain;


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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidencias_mantenimiento")
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "mantenimiento_id",
        nullable = false
    )
    private Mantenimiento mantenimiento;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private TipoEvidenciaMantenimiento tipo;

    @Column(
        name = "nombre_original",
        nullable = false,
        length = 255
    )
    private String nombreOriginal;

    @Column(
        name = "nombre_archivo",
        nullable = false,
        unique = true,
        length = 255
    )
    private String nombreArchivo;

    @Column(
        name = "tipo_contenido",
        nullable = false,
        length = 100
    )
    private String tipoContenido;

    @Column(
        name = "tamanio_bytes",
        nullable = false
    )
    private Long tamanioBytes;

    @Column(length = 1000)
    private String descripcion;

    @Column(
        name = "registrado_por",
        nullable = false,
        length = 150
    )
    private String registradoPor;

    @Column(
        name = "fecha_registro",
        nullable = false
    )
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void antesDeRegistrar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (nombreOriginal != null) {
            nombreOriginal = nombreOriginal.trim();
        }

        if (descripcion != null) {
            descripcion = descripcion.trim();

            if (descripcion.isBlank()) {
                descripcion = null;
            }
        }

        if (registradoPor != null) {
            registradoPor = registradoPor.trim();
        }
    }
}