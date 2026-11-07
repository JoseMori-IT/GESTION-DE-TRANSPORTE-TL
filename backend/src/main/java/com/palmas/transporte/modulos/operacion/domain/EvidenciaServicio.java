package com.palmas.transporte.modulos.operacion.domain;


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
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidencias_servicio")
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "servicio_id",
        nullable = false
    )
    private ServicioTransporte servicio;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_evidencia",
        nullable = false,
        length = 30
    )
    private TipoEvidencia tipo;

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
        length = 120
    )
    private String tipoContenido;

    @Column(
        name = "tamano_bytes",
        nullable = false
    )
    private Long tamanoBytes;

    @Column(length = 500)
    private String descripcion;

    @Column(
        precision = 10,
        scale = 7
    )
    private BigDecimal latitud;

    @Column(
        precision = 10,
        scale = 7
    )
    private BigDecimal longitud;

    @Column(
        name = "nombre_responsable",
        length = 150
    )
    private String nombreResponsable;

    @Column(
        name = "documento_responsable",
        length = 30
    )
    private String documentoResponsable;

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
    public void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}