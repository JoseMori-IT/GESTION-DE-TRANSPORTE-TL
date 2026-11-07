package com.palmas.transporte.modulos.configuracion.api;
/** Comprueba la planta de salida/retorno configurada, sin exponer persistencia. */
public interface BaseOperativa { void validarPlanta(Long inicioId,Long retornoId); }
