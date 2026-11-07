package com.palmas.transporte.modulos.abastecimientos.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="abastecimientos") @Getter @Setter @NoArgsConstructor
public class Abastecimiento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version @Column(name="version_registro",nullable=false) private Long versionRegistro;
 @Column(name="evento_uuid",nullable=false,unique=true,length=36) private String eventoUuid;
 @Column(name="vehiculo_id",nullable=false) private Long vehiculoId;
 @Column(nullable=false,length=30) private String placa;
 @Column(nullable=false) private LocalDateTime fecha;
 @Column(nullable=false,precision=10,scale=3) private BigDecimal galones;
 @Column(name="odometro_km",precision=10,scale=2) private BigDecimal odometroKm;
 @Column(name="motivo_sin_odometro",length=300) private String motivoSinOdometro;
 @Column(nullable=false,length=150) private String grifo;
 @Column(length=100) private String comprobante;
 @Column(length=500) private String observaciones;
 @Column(name="registrado_por",nullable=false,length=150) private String registradoPor;
 @Column(name="fecha_registro",nullable=false) private LocalDateTime fechaRegistro;
 @Column(nullable=false) private boolean anulado;
 @Column(name="anulado_por",length=150) private String anuladoPor;
 @Column(name="fecha_anulacion") private LocalDateTime fechaAnulacion;
 @Column(name="motivo_anulacion",length=300) private String motivoAnulacion;
}
