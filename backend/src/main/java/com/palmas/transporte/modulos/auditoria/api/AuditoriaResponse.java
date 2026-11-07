package com.palmas.transporte.modulos.auditoria.api;


import java.time.LocalDateTime;

import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;

public record AuditoriaResponse(

    Long id,

    Long usuarioId,
    String usuarioNombre,
    String usuarioCorreo,
    String usuarioRol,

    String accion,
    String modulo,

    String entidad,
    String entidadId,
    String codigoReferencia,

    String detalle,
    String valorAnterior,
    String valorNuevo,

    ResultadoAuditoria resultado,

    String direccionIp,
    String userAgent,

    LocalDateTime fechaRegistro
) {
}