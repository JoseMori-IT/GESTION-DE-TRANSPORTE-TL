package com.palmas.transporte.modulos.mantenimiento.domain;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.identidad.domain.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

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
@Table(name = "mantenimientos")
@Getter
@Setter
@NoArgsConstructor
public class Mantenimiento {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * Detecta operaciones simultáneas sobre el mismo
     * registro de mantenimiento.
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
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "vehiculo_id",
        nullable = false
    )
    private Vehiculo vehiculo;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "reportado_por_id",
        nullable = false
    )
    private Usuario reportadoPor;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_falla",
        nullable = false,
        length = 40
    )
    private TipoFalla tipoFalla;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private PrioridadMantenimiento prioridad;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private EstadoMantenimiento estado;

    @Column(
        nullable = false,
        length = 150
    )
    private String titulo;

    @Column(
        nullable = false,
        length = 1500
    )
    private String descripcion;

    @Column(name = "kilometraje_reporte")
    private Integer kilometrajeReporte;

    @Column(length = 1500)
    private String diagnostico;

    @Column(
        name = "trabajo_realizado",
        length = 1500
    )
    private String trabajoRealizado;

    @Column(
        name = "taller_proveedor",
        length = 150
    )
    private String tallerProveedor;

    @Column(
        precision = 12,
        scale = 2
    )
    private BigDecimal costo;

    @Column(
        name = "fecha_reporte",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_inicio_atencion")
    private LocalDateTime fechaInicioAtencion;

    @Column(name = "fecha_reparacion")
    private LocalDateTime fechaReparacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(
        name = "observaciones_cierre",
        length = 1000
    )
    private String observacionesCierre;

    @PrePersist
    public void antesDeRegistrar() {
        normalizar();

        if (estado == null) {
            estado = EstadoMantenimiento.REPORTADO;
        }

        LocalDateTime ahora =
            LocalDateTime.now();

        if (fechaReporte == null) {
            fechaReporte = ahora;
        }

        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    public void antesDeActualizar() {
        normalizar();
        fechaActualizacion = LocalDateTime.now();
    }

    private void normalizar() {
        codigo =
            limpiarMayusculas(codigo);

        titulo =
            limpiarTexto(titulo);

        descripcion =
            limpiarTexto(descripcion);

        diagnostico =
            limpiarTexto(diagnostico);

        trabajoRealizado =
            limpiarTexto(trabajoRealizado);

        tallerProveedor =
            limpiarTexto(tallerProveedor);

        observacionesCierre =
            limpiarTexto(observacionesCierre);
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