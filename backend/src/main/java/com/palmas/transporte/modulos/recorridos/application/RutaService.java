package com.palmas.transporte.modulos.recorridos.application;


import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.recorridos.api.RutaRequest;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;
    private final UbicacionService ubicacionService;

    public RutaService(
        RutaRepository rutaRepository,
        UbicacionService ubicacionService
    ) {
        this.rutaRepository = rutaRepository;
        this.ubicacionService = ubicacionService;
    }

    public List<Ruta> listar() {
        return rutaRepository.findAll(
            Sort.by(Sort.Direction.DESC, "id")
        );
    }

    public Ruta buscarPorId(Long id) {
        return rutaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ruta no encontrada"
            ));
    }

    @Transactional
    public Ruta registrar(RutaRequest request) {
        validarRecorridoDuplicado(request, null);

        Ruta ruta = new Ruta();
        asignarDatos(ruta, request);
        String codigo;
        do { codigo="RUT-"+java.util.UUID.randomUUID().toString().replace("-", "").substring(0,16).toUpperCase(java.util.Locale.ROOT); } while(rutaRepository.existsByCodigoIgnoreCase(codigo));
        ruta.setCodigo(codigo);

        return rutaRepository.save(ruta);
    }

    @Transactional
    public Ruta actualizar(Long id, RutaRequest request) {
        Ruta ruta = buscarPorId(id);
        validarRecorridoDuplicado(request, id);

        asignarDatos(ruta, request);

        return rutaRepository.save(ruta);
    }

    @Transactional
    public Ruta desactivar(Long id) {
        Ruta ruta = buscarPorId(id);
        ruta.setEstado(EstadoRuta.INACTIVA);

        return rutaRepository.save(ruta);
    }

    private void asignarDatos(
            Ruta ruta,
            RutaRequest request) {

        ruta.setNombre(request.nombre().trim());
        Ubicacion origen = ubicacionService.buscarActiva(
            request.origenUbicacionId()
        );
        Ubicacion destino = ubicacionService.buscarActiva(
            request.destinoUbicacionId()
        );
        ruta.setOrigenUbicacion(origen);
        ruta.setDestinoUbicacion(destino);
        ruta.setOrigen(origen.getNombre());
        ruta.setDestino(destino.getNombre());
        ruta.setDistanciaKm(request.distanciaKm());
        ruta.setDuracionEstimadaMinutos(
            request.duracionEstimadaMinutos()
        );
        ruta.setObservaciones(
            normalizarObservaciones(request.observaciones())
        );
        ruta.setEstado(request.estado());
    }

    private void validarRecorridoDuplicado(
        RutaRequest request,
        Long idIgnorado
    ) {
        boolean existe = idIgnorado == null
            ? rutaRepository
                .existsByOrigenUbicacionIdAndDestinoUbicacionId(
                    request.origenUbicacionId(),
                    request.destinoUbicacionId()
                )
            : rutaRepository
                .existsByOrigenUbicacionIdAndDestinoUbicacionIdAndIdNot(
                    request.origenUbicacionId(),
                    request.destinoUbicacionId(),
                    idIgnorado
                );

        if (existe) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una ruta con el mismo origen y destino"
            );
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }

        return observaciones.trim();
    }
}
