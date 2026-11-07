package com.palmas.transporte.modulos.flota.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="movimientos_odometro") @Getter @Setter @NoArgsConstructor
public class MovimientoOdometro {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="vehiculo_id",nullable=false) private Long vehiculoId;
 @Column(name="servicio_id") private Long servicioId;
 @Column(nullable=false,length=30) private String tipo;
 @Column(name="lectura_anterior",precision=10,scale=2) private BigDecimal lecturaAnterior;
 @Column(name="lectura_nueva",precision=10,scale=2) private BigDecimal lecturaNueva;
 @Column(name="estado_anterior",length=20) private String estadoAnterior;
 @Column(name="estado_nuevo",nullable=false,length=20) private String estadoNuevo;
 @Column(length=300) private String motivo;
 @Column(nullable=false,length=150) private String usuario;
 @Column(name="fecha_lectura",nullable=false) private LocalDateTime fechaLectura;
 @Column(name="fecha_registro",nullable=false) private LocalDateTime fechaRegistro;
}
