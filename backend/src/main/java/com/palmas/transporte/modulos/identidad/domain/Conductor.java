package com.palmas.transporte.modulos.identidad.domain;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

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
@Table(name = "conductores")
@Getter
@Setter
@NoArgsConstructor
public class Conductor {

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

    /*
     * Se conserva por compatibilidad con los datos
     * y procesos existentes.
     */
    @Column(length = 8)
    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_documento",
        nullable = false,
        length = 20
    )
    private TipoDocumento tipoDocumento;

    @Column(
        name = "numero_documento",
        length = 30
    )
    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "rol_tripulacion",
        nullable = false,
        length = 20
    )
    private RolTripulacion rolTripulacion;

    @Column(
        nullable = false,
        length = 80
    )
    private String nombres;

    @Column(
        nullable = false,
        length = 80
    )
    private String apellidos;

    @Column(
        nullable = false,
        length = 15
    )
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(
        name = "numero_licencia",
        length = 20
    )
    private String numeroLicencia;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "categoria_licencia",
        length = 20
    )
    private CategoriaLicencia categoriaLicencia;

    @Column(name = "vencimiento_licencia")
    private LocalDate vencimientoLicencia;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private EstadoConductor estado;

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

        if (rolTripulacion == null) {
            rolTripulacion =
                RolTripulacion.CHOFER;
        }

        if (estado == null) {
            estado =
                EstadoConductor.DISPONIBLE;
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
        if (tipoDocumento == null) {
            tipoDocumento = TipoDocumento.DNI;
        }

        dni = limpiarTexto(dni);

        numeroDocumento =
            limpiarMayusculas(numeroDocumento);

        /*
         * Mantiene compatibilidad entre el antiguo campo
         * DNI y el nuevo número de documento.
         */
        if (
            numeroDocumento == null
            && dni != null
        ) {
            numeroDocumento = dni;
        }

        if (
            tipoDocumento == TipoDocumento.DNI
            && dni == null
            && numeroDocumento != null
        ) {
            dni = numeroDocumento;
        }

        nombres = limpiarTexto(nombres);
        apellidos = limpiarTexto(apellidos);
        telefono = limpiarTexto(telefono);

        numeroLicencia =
            limpiarMayusculas(numeroLicencia);

        correo = limpiarMinusculas(correo);
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

    private String limpiarMinusculas(
            String valor) {

        String limpio = limpiarTexto(valor);

        return limpio != null
            ? limpio.toLowerCase(Locale.ROOT)
            : null;
    }
}