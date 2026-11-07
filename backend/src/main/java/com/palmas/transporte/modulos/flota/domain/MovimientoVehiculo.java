package com.palmas.transporte.modulos.flota.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="movimientos_vehiculo") @Getter @Setter @NoArgsConstructor
public class MovimientoVehiculo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="vehiculo_id",nullable=false,updatable=false) private Long vehiculoId;
 @Column(nullable=false,length=20,updatable=false) private String accion;
 @Column(name="estado_anterior",nullable=false,length=30,updatable=false) private String estadoAnterior;
 @Column(name="estado_nuevo",nullable=false,length=30,updatable=false) private String estadoNuevo;
 @Column(name="fecha_efectiva",nullable=false,updatable=false) private LocalDate fechaEfectiva;
 @Column(nullable=false,length=1000,updatable=false) private String motivo;
 @Column(length=2000,updatable=false) private String observaciones;
 @Column(name="referencia_sustento",length=500,updatable=false) private String referenciaSustento;
 @Column(nullable=false,length=150,updatable=false) private String usuario;
 @Column(nullable=false,length=200,updatable=false) private String responsable;
 @Column(nullable=false,updatable=false) private LocalDateTime registrado;
}
