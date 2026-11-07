package com.palmas.transporte.modulos.configuracion.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="configuracion_operativa") @Getter @Setter @NoArgsConstructor
public class ConfiguracionOperativa {
 @Id private Long id;
 @Version @Column(name="version_registro",nullable=false) private Long versionRegistro;
 @Column(nullable=false,length=100) private String sede;
 @Column(name="planta_base_id") private Long plantaBaseId;
 @Column(name="galones_por_km",nullable=false,precision=12,scale=6) private BigDecimal galonesPorKm;
 @Column(name="kg_co2_por_km",nullable=false,precision=12,scale=6) private BigDecimal kgCo2PorKm;
 @Column(name="referencia_combustible",nullable=false,length=500) private String referenciaCombustible;
 @Column(name="referencia_emisiones",nullable=false,length=500) private String referenciaEmisiones;
 @Column(name="actualizado_por",nullable=false,length=150) private String actualizadoPor;
 @Column(name="actualizado_en",nullable=false) private LocalDateTime actualizadoEn;
}
