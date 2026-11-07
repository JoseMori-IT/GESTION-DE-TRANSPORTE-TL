package com.palmas.transporte.modulos.identidad.application;


import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.identidad.api.ConductorRequest;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;

import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.operacion.api.ConsultaServiciosActivos;

@Service
public class ConductorService {

    private final ConductorRepository conductorRepository;
    private final ConsultaServiciosActivos servicioRepository;

    public ConductorService(
        ConductorRepository conductorRepository,
        ConsultaServiciosActivos servicioRepository
    ) {
        this.conductorRepository = conductorRepository;
        this.servicioRepository = servicioRepository;
    }

    public List<Conductor> listar() {
        return conductorRepository.findAll(
            Sort.by(Sort.Direction.DESC, "id")
        );
    }

    public Conductor buscarPorId(Long id) {
        return conductorRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Personal no encontrado"
            ));
    }

    @Transactional
    public Conductor registrar(ConductorRequest request) {
        if (request.estado() == EstadoConductor.EN_SERVICIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El estado EN_SERVICIO se controla desde el servicio de transporte");
        }
        TipoDocumento tipo = obtenerTipo(request);
        String numero = obtenerNumero(request);
        String licencia = normalizarLicencia(
            request.numeroLicencia()
        );

        validarLicencia(request, licencia);
        validarDocumentoDisponible(tipo, numero, null);
        validarLicenciaDisponible(licencia, null);

        Conductor conductor = new Conductor();
        asignarDatos(conductor, request, tipo, numero, licencia);

        return conductorRepository.save(conductor);
    }

    @Transactional
    public Conductor actualizar(
        Long id,
        ConductorRequest request
    ) {
        Conductor conductor = conductorRepository.bloquearPorId(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));

        validarCambioManual(conductor, request.estado(), obtenerRol(request));

        TipoDocumento tipo = obtenerTipo(request);
        String numero = obtenerNumero(request);
        String licencia = normalizarLicencia(
            request.numeroLicencia()
        );

        validarLicencia(request, licencia);
        validarDocumentoDisponible(tipo, numero, id);
        validarLicenciaDisponible(licencia, id);

        asignarDatos(conductor, request, tipo, numero, licencia);

        return conductorRepository.save(conductor);
    }

    @Transactional
    public Conductor desactivar(Long id) {
        Conductor conductor = conductorRepository.bloquearPorId(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));
        validarCambioManual(conductor, EstadoConductor.INACTIVO, conductor.getRolTripulacion());
        conductor.setEstado(EstadoConductor.INACTIVO);

        return conductorRepository.save(conductor);
    }

    private void validarCambioManual(
        Conductor conductor, EstadoConductor nuevoEstado, RolTripulacion nuevoRol
    ) {
        boolean cambiaEstado = conductor.getEstado() != nuevoEstado;
        boolean cambiaRol = conductor.getRolTripulacion() != nuevoRol;

        if (!cambiaEstado && !cambiaRol) {
            return; // Permitir editar datos personales sin alterar la asignacion.
        }

        if (cambiaEstado && (nuevoEstado == EstadoConductor.EN_SERVICIO
            || conductor.getEstado() == EstadoConductor.EN_SERVICIO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El estado EN_SERVICIO se controla desde el servicio de transporte");
        }

        boolean reservado = servicioRepository.personaTieneAsignacion(conductor.getId());

        if (reservado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El personal tiene un servicio programado o en curso; no se puede cambiar su estado ni rol");
        }
    }

    private void asignarDatos(
        Conductor conductor,
        ConductorRequest request,
        TipoDocumento tipo,
        String numero,
        String licencia
    ) {
        conductor.setTipoDocumento(tipo);
        conductor.setNumeroDocumento(numero);
        conductor.setRolTripulacion(obtenerRol(request));

        // Se conserva el campo antiguo solo para los DNI.
        conductor.setDni(
            tipo == TipoDocumento.DNI ? numero : null
        );

        conductor.setNombres(request.nombres().trim());
        conductor.setApellidos(request.apellidos().trim());
        conductor.setTelefono(request.telefono().trim());
        conductor.setCorreo(normalizarCorreo(request.correo()));

        conductor.setNumeroLicencia(licencia);
        conductor.setCategoriaLicencia(
            request.categoriaLicencia()
        );
        conductor.setVencimientoLicencia(
            request.vencimientoLicencia()
        );

        conductor.setEstado(request.estado());
    }

    private TipoDocumento obtenerTipo(ConductorRequest request) {
        return request.tipoDocumento() == null
            ? TipoDocumento.DNI
            : request.tipoDocumento();
    }

    private RolTripulacion obtenerRol(ConductorRequest request) {
        return request.rolTripulacion() == null
            ? RolTripulacion.CHOFER
            : request.rolTripulacion();
    }

    private String obtenerNumero(ConductorRequest request) {
        String numero = request.numeroDocumento();

        if (numero == null || numero.isBlank()) {
            numero = request.dni();
        }

        if (numero == null || numero.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El documento de identidad es obligatorio"
            );
        }

        return numero.trim().toUpperCase();
    }

    private void validarDocumentoDisponible(
        TipoDocumento tipo,
        String numero,
        Long idActual
    ) {
        boolean duplicado = idActual == null
            ? conductorRepository
                .existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(
                    tipo, numero
                )
            : conductorRepository
                .existsByTipoDocumentoAndNumeroDocumentoIgnoreCaseAndIdNot(
                    tipo, numero, idActual
                );

        if (duplicado) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe personal con este tipo y número de documento"
            );
        }
    }

    private void validarLicenciaDisponible(
        String licencia,
        Long idActual
    ) {
        if (licencia == null) {
            return;
        }

        boolean duplicada = idActual == null
            ? conductorRepository
                .existsByNumeroLicenciaIgnoreCase(licencia)
            : conductorRepository
                .existsByNumeroLicenciaIgnoreCaseAndIdNot(
                    licencia, idActual
                );

        if (duplicada) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe personal con este número de licencia"
            );
        }
    }

    private void validarLicencia(
        ConductorRequest request,
        String licencia
    ) {
        RolTripulacion rol = obtenerRol(request);

        boolean tieneLicenciaCompleta =
            licencia != null
            && request.categoriaLicencia() != null
            && request.vencimientoLicencia() != null;

        if (rol != RolTripulacion.COPILOTO
            && !tieneLicenciaCompleta) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El chofer requiere licencia, categoría y vencimiento"
            );
        }
    }

    private String normalizarLicencia(String licencia) {
        if (licencia == null || licencia.isBlank()) {
            return null;
        }

        return licencia.trim().toUpperCase();
    }

    private String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }

        return correo.trim().toLowerCase();
    }
}
