package com.palmas.transporte.modulos.flota.application;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;

import java.util.List;
import java.util.UUID;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import com.palmas.transporte.modulos.flota.api.VehiculoRequest;

import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.flota.domain.FotoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.flota.persistence.FotoVehiculoRepository;
import com.palmas.transporte.modulos.operacion.api.ConsultaServiciosActivos;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final ConsultaServiciosActivos servicioRepository;
    private final FotoVehiculoRepository fotoRepository;
    private final OdometroService odometros;

    public VehiculoService(
            VehiculoRepository vehiculoRepository,
            ConsultaServiciosActivos servicioRepository,
            FotoVehiculoRepository fotoRepository, OdometroService odometros) {

        this.vehiculoRepository = vehiculoRepository;
        this.servicioRepository = servicioRepository;
        this.fotoRepository = fotoRepository; this.odometros=odometros;
    }

    public List<Vehiculo> listar() {
        return vehiculoRepository.findAll(
            Sort.by(Sort.Direction.DESC, "id")
        );
    }

    public Vehiculo buscarPorId(Long id) {
        return vehiculoRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Vehículo no encontrado"
            ));
    }

    @Transactional
    public Vehiculo registrar(VehiculoRequest request) {
        String placa = normalizar(request.placa());

        if (vehiculoRepository.existsByPlacaIgnoreCase(placa)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un vehículo registrado con esta placa"
            );
        }

        validarEstadoManual(request.estado());
        if(request.estado() == EstadoVehiculo.MANTENIMIENTO || request.estado() == EstadoVehiculo.SUSPENDIDO || request.estado() == EstadoVehiculo.BAJA_DEFINITIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Registra la unidad y luego envíala a mantenimiento desde Flota, indicando prioridad y falla.");
        }

        odometros.validarAlta(request);
        Vehiculo vehiculo = new Vehiculo();
        asignarDatos(vehiculo, request);
        vehiculo.setCodigoInterno(
            "TMP-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase()
        );
        vehiculo.setEstado(request.estado());

        vehiculoRepository.saveAndFlush(vehiculo);

        odometros.registrarUnidad(vehiculo,request,true);
        String codigo = String.format(
            "VEH-%06d",
            vehiculo.getId()
        );

        if (vehiculoRepository
                .existsByCodigoInternoIgnoreCase(codigo)) {
            codigo = "VEH-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
        }

        vehiculo.setCodigoInterno(codigo);
        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public Vehiculo actualizar(
            Long id,
            VehiculoRequest request) {

        Vehiculo vehiculo = bloquearPorId(id);
        if(request.estado() != vehiculo.getEstado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El estado se gestiona desde Flota y Mantenimiento. Actualiza la unidad y registra la atención desde ese flujo.");
        }

        String placa = normalizar(request.placa());

        if (vehiculoRepository
                .existsByPlacaIgnoreCaseAndIdNot(placa, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe otro vehículo registrado con esta placa"
            );
        }

        /*
         * La edición solo modifica los datos generales.
         * El estado se conserva y se cambia mediante
         * el endpoint específico de estado.
         */
        EstadoVehiculo estadoActual = vehiculo.getEstado();

        odometros.registrarUnidad(vehiculo,request,false);
        asignarDatos(vehiculo, request);
        vehiculo.setEstado(estadoActual);

        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public Vehiculo cambiarEstado(
            Long id,
            EstadoVehiculo nuevoEstado) {

        Vehiculo vehiculo = bloquearPorId(id);
        EstadoVehiculo estadoActual = vehiculo.getEstado();

        if (estadoActual == nuevoEstado) {
            return vehiculo;
        }

        if(estadoActual == EstadoVehiculo.BAJA_DEFINITIVA || nuevoEstado == EstadoVehiculo.BAJA_DEFINITIVA || estadoActual == EstadoVehiculo.SUSPENDIDO || nuevoEstado == EstadoVehiculo.SUSPENDIDO)throw new ResponseStatusException(HttpStatus.CONFLICT,"La baja definitiva requiere su proceso separado con motivo, confirmación e historial.");
        validarEstadoManual(nuevoEstado);

        boolean tieneServicioActivo =
            servicioRepository.unidadTieneAsignacion(id);

        if (tieneServicioActivo) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El vehículo tiene un servicio programado o en curso"
            );
        }

        if (estadoActual == EstadoVehiculo.EN_SERVICIO) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El estado EN_SERVICIO se controla desde el traslado"
            );
        }

        vehiculo.setEstado(nuevoEstado);

        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public Vehiculo cambiarEstadoManual(Long id, EstadoVehiculo nuevoEstado) {
        Vehiculo unidad = bloquearPorId(id);
        if (nuevoEstado != unidad.getEstado()
                && (nuevoEstado == EstadoVehiculo.INACTIVO || nuevoEstado == EstadoVehiculo.MANTENIMIENTO
                    || unidad.getEstado() == EstadoVehiculo.MANTENIMIENTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El ingreso y la salida de mantenimiento requieren registrar o cerrar la atención correspondiente.");
        }
        return cambiarEstado(id, nuevoEstado);
    }

    @Transactional
    public Vehiculo desactivar(Long id) {
        return cambiarEstadoManual(id, EstadoVehiculo.INACTIVO);
    }

    @Transactional
    public Vehiculo guardarFoto(
            Long id,
            MultipartFile archivo) {

        Vehiculo vehiculo = bloquearPorId(id);
        FotoValidada foto = validarFoto(archivo);

        FotoVehiculo registro = fotoRepository
            .findById(id)
            .orElseGet(FotoVehiculo::new);

        registro.setVehiculo(vehiculo);
        registro.setContenido(foto.contenido());
        registro.setTipoContenido(foto.tipoContenido());
        fotoRepository.save(registro);

        vehiculo.setTieneFoto(true);
        return vehiculoRepository.save(vehiculo);
    }

    @Transactional(readOnly = true)
    public FotoDescargable obtenerFoto(Long id) {
        buscarPorId(id);

        com.palmas.transporte.modulos.flota.domain.FotoVehiculo foto =
            fotoRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El vehículo no tiene una foto registrada"
                    )
                );

        return new FotoDescargable(
            foto.getContenido(),
            foto.getTipoContenido()
        );
    }

    @Transactional
    public Vehiculo eliminarFoto(Long id) {
        Vehiculo vehiculo = bloquearPorId(id);
        fotoRepository.deleteById(id);
        vehiculo.setTieneFoto(false);
        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public Vehiculo bloquearPorId(Long id) {
        return vehiculoRepository.bloquearPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("No se encontro el vehiculo."));
    }

    private void asignarDatos(
            Vehiculo vehiculo,
            VehiculoRequest request) {

        vehiculo.setPlaca(normalizar(request.placa()));
        vehiculo.setMarca(request.marca().trim());
        vehiculo.setModelo(request.modelo().trim());
        vehiculo.setAnioFabricacion(
            request.anioFabricacion()
        );
        vehiculo.setTipo(request.tipo());
        vehiculo.setCapacidadPasajeros(
            request.capacidadPasajeros()
        );
    }

    private void validarEstadoManual(
            EstadoVehiculo estado) {

        if (estado == EstadoVehiculo.EN_SERVICIO) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "EN_SERVICIO se asigna automáticamente al iniciar un traslado"
            );
        }
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase();
    }

    private FotoValidada validarFoto(
            MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selecciona una foto del vehículo"
            );
        }

        if (archivo.getSize() > 5L * 1024L * 1024L) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La foto no puede superar los 5 MB"
            );
        }

        try {
            byte[] contenido = archivo.getBytes();
            String tipo = detectarTipo(contenido);
            BufferedImage imagen = ImageIO.read(
                new ByteArrayInputStream(contenido)
            );

            if (imagen == null) {
                throw fotoInvalida();
            }

            if (
                imagen.getWidth() > 4096
                || imagen.getHeight() > 4096
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La foto no puede superar 4096 x 4096 píxeles"
                );
            }

            return new FotoValidada(contenido, tipo);
        } catch (IOException exception) {
            throw fotoInvalida();
        }
    }

    private String detectarTipo(byte[] contenido) {
        boolean jpeg = contenido.length >= 3
            && (contenido[0] & 0xff) == 0xff
            && (contenido[1] & 0xff) == 0xd8
            && (contenido[2] & 0xff) == 0xff;

        boolean png = contenido.length >= 8
            && (contenido[0] & 0xff) == 0x89
            && contenido[1] == 0x50
            && contenido[2] == 0x4e
            && contenido[3] == 0x47
            && contenido[4] == 0x0d
            && contenido[5] == 0x0a
            && contenido[6] == 0x1a
            && contenido[7] == 0x0a;

        if (jpeg) {
            return "image/jpeg";
        }

        if (png) {
            return "image/png";
        }

        throw fotoInvalida();
    }

    private ResponseStatusException fotoInvalida() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La foto debe ser una imagen JPG o PNG válida"
        );
    }

    private record FotoValidada(
        byte[] contenido,
        String tipoContenido
    ) {
    }

    public record FotoDescargable(
        byte[] contenido,
        String tipoContenido
    ) {
    }
}
