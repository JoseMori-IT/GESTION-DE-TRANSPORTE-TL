package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PersonalAdministrativoResponse(

    Long id,

    String dni,

    String nombres,

    String apellidos,

    String nombreCompleto,

    String correo,

    String cargo,

    String rol,

    Boolean activo,

    LocalDate fechaIngreso,

    LocalDate fechaCese,

    String motivoDesactivacion,

    LocalDateTime fechaCreacion,

    LocalDateTime fechaActualizacion,

    LocalDateTime fechaPrimerLogin,

    LocalDateTime fechaUltimoLogin,

    LocalDateTime fechaUltimaConexion,

    String ultimaDireccionIp,

    String ultimoUserAgent,

    List<String> procesosPermitidos

) {
}