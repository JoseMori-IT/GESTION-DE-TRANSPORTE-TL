package com.palmas.transporte.modulos.identidad.api;


public record LoginResponse(

    Long id,

    String nombres,

    String correo,

    String rol,

    Long conductorId,

    String token,

    String mensaje,

    boolean cambioContrasenaRequerido

) {
    @Override public String toString() { return "LoginResponse[datos protegidos]"; }

}
