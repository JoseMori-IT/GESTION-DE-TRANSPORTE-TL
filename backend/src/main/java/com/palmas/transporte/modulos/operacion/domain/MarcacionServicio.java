package com.palmas.transporte.modulos.operacion.domain;
import com.palmas.transporte.modulos.identidad.domain.Conductor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "marcaciones_servicio",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_marcaciones_evento_uuid",
            columnNames = "evento_uuid"
        ),
        @UniqueConstraint(
            name = "UQ_marcaciones_servicio_tipo",
            columnNames = {"servicio_id", "tipo"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MarcacionServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_uuid", nullable = false, length = 36)
    private String eventoUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioTransporte servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMarcacionServicio tipo;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "precision_metros", nullable = false, precision = 8, scale = 2)
    private BigDecimal precisionMetros;

    @Column(name = "fecha_hora_dispositivo", nullable = false)
    private LocalDateTime fechaHoraDispositivo;

    @Column(name = "fecha_hora_servidor", nullable = false, updatable = false)
    private LocalDateTime fechaHoraServidor;

    @Column(name = "registrado_offline", nullable = false)
    private Boolean registradoOffline;

    @Column(length = 300)
    private String observacion;

    @PrePersist
    public void antesDeRegistrar() {
        if (eventoUuid == null || eventoUuid.isBlank()) {
            eventoUuid = UUID.randomUUID().toString();
        }
        if (fechaHoraServidor == null) {
            fechaHoraServidor = LocalDateTime.now();
        }
        if (registradoOffline == null) {
            registradoOffline = false;
        }
        if (observacion != null) {
            observacion = observacion.trim();
            if (observacion.isBlank()) {
                observacion = null;
            }
        }
    }
}
