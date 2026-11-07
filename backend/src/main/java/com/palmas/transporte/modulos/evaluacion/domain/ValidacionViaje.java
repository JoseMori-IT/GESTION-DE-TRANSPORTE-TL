package com.palmas.transporte.modulos.evaluacion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="validaciones_viaje") @Getter @Setter @NoArgsConstructor
public class ValidacionViaje {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="servicio_id",nullable=false,updatable=false) private Long servicioId;
 @Column(nullable=false,length=20,updatable=false) private String estado;
 @Column(length=1000,updatable=false) private String comentario;
 @Column(nullable=false,length=150,updatable=false) private String usuario;
 @Column(nullable=false,length=200,updatable=false) private String responsable;
 @Column(nullable=false,updatable=false) private LocalDateTime fecha;
}
