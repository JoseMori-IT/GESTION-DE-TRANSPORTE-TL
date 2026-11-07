package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;

import com.palmas.transporte.modulos.identidad.domain.CategoriaLicencia;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConductorRequest(

    // Campo antiguo: se conserva mientras adaptamos el frontend.
    @Pattern(
        regexp = "^\\d{8}$",
        message = "El DNI debe contener exactamente 8 números"
    )
    String dni,

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 80)
    String nombres,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 80)
    String apellidos,

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15, message = "El teléfono admite hasta 15 caracteres")
    @Pattern(
        regexp = "^\\+?[0-9]{7,14}$",
        message = "El teléfono debe contener entre 7 y 14 números"
    )
    String telefono,

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 150)
    String correo,

    @Pattern(
        regexp = "^[A-Za-z0-9-]{6,20}$",
        message = "El número de licencia no tiene un formato válido"
    )
    String numeroLicencia,

    CategoriaLicencia categoriaLicencia,

    @FutureOrPresent(message = "La licencia debe estar vigente")
    LocalDate vencimientoLicencia,

    @NotNull(message = "El estado es obligatorio")
    EstadoConductor estado,

    TipoDocumento tipoDocumento,

    @Size(max = 30, message = "El documento admite hasta 30 caracteres")
    String numeroDocumento,

    RolTripulacion rolTripulacion

) {
    @AssertTrue(message = "Ingresa un documento de identidad válido")
    public boolean isDocumentoValido() {
        TipoDocumento tipo = tipoDocumento == null
            ? TipoDocumento.DNI
            : tipoDocumento;

        String numero = numeroDocumento;

        if (numero == null || numero.isBlank()) {
            numero = dni;
        }

        if (numero == null) {
            return false;
        }

        numero = numero.trim();

        if (tipo == TipoDocumento.DNI) {
            return numero.matches("\\d{8}")
                && (dni == null || dni.equals(numero));
        }

        return numero.matches("[A-Za-z0-9-]{4,30}");
    }

    @AssertTrue(
        message = "Para conducir se requiere número, categoría y vencimiento de licencia"
    )
    public boolean isLicenciaValidaParaElRol() {
        RolTripulacion rol = rolTripulacion == null
            ? RolTripulacion.CHOFER
            : rolTripulacion;

        boolean tieneNumero = numeroLicencia != null
            && !numeroLicencia.isBlank();

        boolean tieneCategoria = categoriaLicencia != null;
        boolean tieneVencimiento = vencimientoLicencia != null;

        if (rol == RolTripulacion.COPILOTO
            && !tieneNumero
            && !tieneCategoria
            && !tieneVencimiento) {
            return true;
        }

        return tieneNumero && tieneCategoria && tieneVencimiento;
    }
}