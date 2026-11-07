package com.palmas.transporte.modulos.identidad.api;


public record UsuarioResponse(

    Long id,

    String nombres,

    String correo,

    String rol,

    Boolean activo,

    Long conductorId,

    String nombreConductor,

    String dni

) {
}
