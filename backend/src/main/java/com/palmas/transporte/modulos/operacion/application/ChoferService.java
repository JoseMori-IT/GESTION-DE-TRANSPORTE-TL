package com.palmas.transporte.modulos.operacion.application;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

@Service
public class ChoferService {

    private final ServicioTransporteRepository
        servicioRepository;

    private final ServicioTransporteService
        servicioTransporteService;

    public ChoferService(
            ServicioTransporteRepository servicioRepository,
            ServicioTransporteService servicioTransporteService) {

        this.servicioRepository = servicioRepository;
        this.servicioTransporteService =
            servicioTransporteService;
    }

    @Transactional(readOnly = true)
    public List<ServicioTransporte> listarServicios(
            Long conductorId) {

        return servicioRepository
            .findByConductorIdOrderByIdDesc(conductorId);
    }

    @Transactional
    public ServicioTransporte cambiarEstado(
            Long conductorId,
            Long servicioId,
            EstadoServicio nuevoEstado) {

        servicioRepository
            .findByIdAndConductorId(
                servicioId,
                conductorId
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El servicio no existe o no está asignado al chofer autenticado"
                )
            );

        if (
            nuevoEstado != EstadoServicio.EN_CURSO
            && nuevoEstado != EstadoServicio.COMPLETADO
            && nuevoEstado != EstadoServicio.CANCELADO
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El chofer no puede asignar el estado solicitado"
            );
        }

        return servicioTransporteService.cambiarEstado(
            servicioId,
            nuevoEstado
        );
    }
}