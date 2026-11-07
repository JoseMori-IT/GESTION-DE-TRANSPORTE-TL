package com.palmas.transporte.modulos.recorridos.api;
public interface CatalogoUbicaciones {
 Punto consultar(Long id);
 record Punto(Long id,String nombre,boolean plantaActiva) {}
}
