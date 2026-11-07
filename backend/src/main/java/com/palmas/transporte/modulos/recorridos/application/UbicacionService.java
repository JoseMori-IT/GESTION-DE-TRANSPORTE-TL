package com.palmas.transporte.modulos.recorridos.application;


import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.recorridos.api.UbicacionRequest;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;

@Service
public class UbicacionService {

    private static final double RADIO_DUPLICADO_METROS = 50.0;

    private final UbicacionRepository ubicacionRepository;

    public UbicacionService(
        UbicacionRepository ubicacionRepository
    ) {
        this.ubicacionRepository = ubicacionRepository;
    }

    public List<Ubicacion> listar() {
        return ubicacionRepository.findAll(
            Sort.by(Sort.Direction.ASC, "nombre")
        );
    }

    public Ubicacion buscarPorId(Long id) {
        return ubicacionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ubicación no encontrada"
            ));
    }

    public Ubicacion buscarActiva(Long id) {
        Ubicacion ubicacion = buscarPorId(id);
        if (ubicacion.getEstado() != EstadoRuta.ACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La ubicación seleccionada se encuentra inactiva"
            );
        }
        return ubicacion;
    }

    @Transactional
    public Ubicacion registrar(UbicacionRequest request) {
        validarNombre(request.nombre(), null);
        validarCercania(request, null);

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setCodigo(generarCodigo());
        asignarDatos(ubicacion, request);
        return ubicacionRepository.save(ubicacion);
    }

    @Transactional
    public Ubicacion actualizar(
        Long id,
        UbicacionRequest request
    ) {
        Ubicacion ubicacion = buscarPorId(id);
        validarNombre(request.nombre(), id);
        validarCercania(request, id);
        asignarDatos(ubicacion, request);
        return ubicacionRepository.save(ubicacion);
    }

    @Transactional
    public Ubicacion desactivar(Long id) {
        Ubicacion ubicacion = buscarPorId(id);
        ubicacion.setEstado(EstadoRuta.INACTIVA);
        return ubicacionRepository.save(ubicacion);
    }

    private void validarNombre(String nombre, Long id) {
        String limpio = nombre.trim();
        boolean existe = id == null
            ? ubicacionRepository.existsByNombreIgnoreCase(limpio)
            : ubicacionRepository.existsByNombreIgnoreCaseAndIdNot(
                limpio,
                id
            );
        if (existe) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una ubicación registrada con este nombre"
            );
        }
    }

    private void validarCercania(
        UbicacionRequest request,
        Long idIgnorado
    ) {
        for (Ubicacion existente : ubicacionRepository.findAll()) {
            if (existente.getId().equals(idIgnorado)
                || existente.getLatitud() == null
                || existente.getLongitud() == null) {
                continue;
            }

            double metros = distanciaMetros(
                request.latitud().doubleValue(),
                request.longitud().doubleValue(),
                existente.getLatitud().doubleValue(),
                existente.getLongitud().doubleValue()
            );

            if (metros < RADIO_DUPLICADO_METROS) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe la ubicación "
                        + existente.getNombre()
                        + " a aproximadamente "
                        + Math.round(metros)
                        + " metros"
                );
            }
        }
    }

    private double distanciaMetros(
        double latitud1,
        double longitud1,
        double latitud2,
        double longitud2
    ) {
        double radioTierra = 6371000.0;
        double lat1 = Math.toRadians(latitud1);
        double lat2 = Math.toRadians(latitud2);
        double deltaLat = Math.toRadians(latitud2 - latitud1);
        double deltaLon = Math.toRadians(longitud2 - longitud1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return radioTierra * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void asignarDatos(
        Ubicacion ubicacion,
        UbicacionRequest request
    ) {
        ubicacion.setNombre(request.nombre().trim());
        ubicacion.setTipo(request.tipo());
        ubicacion.setLatitud(request.latitud());
        ubicacion.setLongitud(request.longitud());
        ubicacion.setReferencia(limpiar(request.referencia()));
        ubicacion.setEstado(request.estado());
    }

    private String generarCodigo() {
        String codigo;
        do {
            codigo = "UBI-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        } while (ubicacionRepository.existsByCodigoIgnoreCase(codigo));
        return codigo;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank()
            ? null
            : valor.trim();
    }
}
