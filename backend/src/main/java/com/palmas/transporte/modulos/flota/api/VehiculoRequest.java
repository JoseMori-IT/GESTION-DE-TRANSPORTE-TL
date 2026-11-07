package com.palmas.transporte.modulos.flota.api;


import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehiculoRequest(

    // Se conserva por compatibilidad con clientes antiguos.
    // El backend genera el código y no utiliza este valor.
    String codigoInterno,

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(
        regexp = "^[A-Za-z0-9-]{6,15}$",
        message = "La placa debe contener entre 6 y 15 caracteres"
    )
    String placa,

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 60, message = "La marca admite hasta 60 caracteres")
    String marca,

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 60, message = "El modelo admite hasta 60 caracteres")
    String modelo,

    @NotNull(message = "El año de fabricación es obligatorio")
    @Min(value = 1980, message = "El año debe ser igual o posterior a 1980")
    @Max(value = 2100, message = "El año ingresado no es válido")
    Integer anioFabricacion,

    @NotNull(message = "El tipo de vehículo es obligatorio")
    TipoVehiculo tipo,


    @Min(value = 1, message = "La capacidad mínima es 1")
    @Max(value = 100, message = "La capacidad máxima es 100")
    Integer capacidadPasajeros,

    @NotNull(message = "El estado es obligatorio")
    EstadoVehiculo estado,
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("99999999.99")
    @jakarta.validation.constraints.Digits(integer=8,fraction=2) java.math.BigDecimal odometroKm,
    @Pattern(regexp="OPERATIVO|AVERIADO") String estadoOdometro,
    @Size(max=300) String motivoOdometro,
    Boolean actualizarOdometro,
    Long versionRegistro
) {
    public VehiculoRequest(String codigoInterno,String placa,String marca,String modelo,Integer anioFabricacion,TipoVehiculo tipo,Integer capacidadPasajeros,EstadoVehiculo estado){
        this(codigoInterno,placa,marca,modelo,anioFabricacion,tipo,capacidadPasajeros,estado,null,null,null,false,null);
    }
}
