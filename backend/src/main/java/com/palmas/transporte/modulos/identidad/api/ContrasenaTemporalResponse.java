package com.palmas.transporte.modulos.identidad.api;

import java.time.LocalDateTime;

public record ContrasenaTemporalResponse(
    Long usuarioId, String identificador, String contrasenaTemporal,
    LocalDateTime venceEn
) {
    @Override public String toString() { return "ContrasenaTemporalResponse[datos protegidos]"; }
}
