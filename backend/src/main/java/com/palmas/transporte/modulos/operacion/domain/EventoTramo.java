package com.palmas.transporte.modulos.operacion.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "eventos_tramo", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_evento_tramo_uuid", columnNames = "evento_uuid"),
    @UniqueConstraint(name = "UQ_evento_tramo_tipo", columnNames = {"tramo_id", "tipo"})
})
@Getter @Setter @NoArgsConstructor
public class EventoTramo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tramo_id", nullable = false) private TramoServicio tramo;
    @Column(name = "evento_uuid", nullable = false, length = 36) private String eventoUuid;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TipoMarcacionServicio tipo;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitud;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitud;
    @Column(name = "precision_metros", nullable = false, precision = 8, scale = 2) private BigDecimal precisionMetros;
    @Column(name = "fecha_hora_dispositivo", nullable = false) private LocalDateTime fechaHoraDispositivo;
    @Column(name = "fecha_hora_servidor", nullable = false) private LocalDateTime fechaHoraServidor;
    @Column(name = "registrado_offline", nullable = false) private Boolean registradoOffline;
    @Column(length = 300) private String observacion;
}
