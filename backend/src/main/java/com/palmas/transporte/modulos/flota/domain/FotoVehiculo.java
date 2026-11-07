package com.palmas.transporte.modulos.flota.domain;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fotos_vehiculos")
@Getter
@Setter
@NoArgsConstructor
public class FotoVehiculo {

    @Id
    @Column(name = "vehiculo_id")
    private Long vehiculoId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @Lob
    @Column(name = "contenido", nullable = false)
    private byte[] contenido;

    @Column(
        name = "tipo_contenido",
        nullable = false,
        length = 30
    )
    private String tipoContenido;

    @Column(
        name = "fecha_actualizacion",
        nullable = false
    )
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    public void asignarFechaActualizacion() {
        fechaActualizacion = LocalDateTime.now();
    }
}
