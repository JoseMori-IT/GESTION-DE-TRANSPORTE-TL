package com.palmas.transporte.modulos.operacion.domain;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "tramos_servicio", uniqueConstraints = @UniqueConstraint(name = "UQ_tramos_servicio_orden", columnNames = {"servicio_id", "orden"}))
@Getter @Setter @NoArgsConstructor
public class TramoServicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "servicio_id", nullable = false) @JsonIgnore
    private ServicioTransporte servicio;
    @Column(nullable = false) private Integer orden;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "ruta_id") private Ruta ruta;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "origen_ubicacion_id") private Ubicacion origenUbicacion;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "destino_ubicacion_id") private Ubicacion destinoUbicacion;
    @Column(nullable = false, length = 150) private String origen;
    @Column(nullable = false, length = 150) private String destino;
    @Column(name = "producto_transportado", nullable = false, length = 40) private String productoTransportado;
    @Column(nullable = false, length = 20) private String actividad;
    @Column(length = 300) private String indicaciones;
    @Column(name="odometro_no_disponible_salida") private Boolean odometroNoDisponibleSalida;
    @Column(name="odometro_no_disponible_llegada") private Boolean odometroNoDisponibleLlegada;
    @Column(name="motivo_odometro_salida",length=300) private String motivoOdometroSalida;
    @Column(name="motivo_odometro_llegada",length=300) private String motivoOdometroLlegada;
    @Column(name = "odometro_salida_km", precision = 10, scale = 2) private BigDecimal odometroSalidaKm;
    @Column(name = "odometro_llegada_km", precision = 10, scale = 2) private BigDecimal odometroLlegadaKm;
    @Column(name = "salida_real") private LocalDateTime salidaReal;
    @Column(name = "llegada_real") private LocalDateTime llegadaReal;
    @Column(name = "ultima_latitud", precision = 10, scale = 7) private BigDecimal ultimaLatitud;
    @Column(name = "ultima_longitud", precision = 10, scale = 7) private BigDecimal ultimaLongitud;
    @Column(name = "distancia_estimada_km", precision = 10, scale = 2) private BigDecimal distanciaEstimadaKm;
    @Column(name = "duracion_estimada_minutos") private Integer duracionEstimadaMinutos;
    @Column(name = "fuente_distancia", length = 20) private String fuenteDistancia;
    @Column(name = "recorrido_json", columnDefinition = "nvarchar(max)") private String recorridoJson;
}
