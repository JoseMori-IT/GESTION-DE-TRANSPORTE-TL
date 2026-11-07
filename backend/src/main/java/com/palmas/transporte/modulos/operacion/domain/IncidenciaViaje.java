package com.palmas.transporte.modulos.operacion.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Table(name="incidencias_viaje") @Getter @Setter @NoArgsConstructor
public class IncidenciaViaje {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version @Column(name="version_registro",nullable=false) private Long versionRegistro;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="servicio_id",nullable=false) @JsonIgnore private ServicioTransporte servicio;
 @Column(nullable=false,length=40) private String estado;
 @Column(length=4000) private String historial;
 @Column(nullable=false,length=1000) private String descripcion;
 @Column(nullable=false,length=150) private String lugar;
 @Column(precision=10,scale=7) private BigDecimal latitud;
 @Column(precision=10,scale=7) private BigDecimal longitud;
 @Column(name="fecha_reporte",nullable=false) private LocalDateTime fechaReporte;
 @Column(name="fecha_atencion") private LocalDateTime fechaAtencion;
 @Column(name="fecha_reparacion") private LocalDateTime fechaReparacion;
 @Column(name="fecha_resolucion") private LocalDateTime fechaResolucion;
 @Column(name="reportado_por",nullable=false,length=150) private String reportadoPor;
 @Column(name="gestionado_por",length=150) private String gestionadoPor;
 @Column(name="resuelto_por",length=150) private String resueltoPor;
 @Column(length=150) private String tecnico;
 @Column(length=1500) private String diagnostico;
 @Column(name="servicio_auxilio_id") private Long servicioAuxilioId;
 @Column(name="mantenimiento_id") private Long mantenimientoId;
 @Column(name="odometro_regreso_km",precision=10,scale=2) private BigDecimal odometroRegresoKm;
 public boolean activa(){return !"RESUELTA".equals(estado)&&!"RETORNO_CONFIRMADO".equals(estado);}
}
