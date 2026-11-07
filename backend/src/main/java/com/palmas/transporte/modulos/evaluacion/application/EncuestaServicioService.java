package com.palmas.transporte.modulos.evaluacion.application;
import com.palmas.transporte.modulos.identidad.domain.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.evaluacion.api.EncuestaServicioRequest;
import com.palmas.transporte.modulos.evaluacion.api.EncuestaServicioResponse;
import com.palmas.transporte.modulos.evaluacion.domain.EncuestaServicio;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.evaluacion.domain.TipoEvaluador;
import com.palmas.transporte.modulos.evaluacion.persistence.EncuestaServicioRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

@Service
public class EncuestaServicioService {

    private final EncuestaServicioRepository encuestaRepository;
    private final ServicioTransporteRepository servicioRepository;

    public EncuestaServicioService(
            EncuestaServicioRepository encuestaRepository,
            ServicioTransporteRepository servicioRepository) {

        this.encuestaRepository = encuestaRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public List<EncuestaServicioResponse> listar() {
        return encuestaRepository.findAll(
            Sort.by(
                Sort.Direction.DESC,
                "fechaRegistro"
            )
        )
        .stream()
        .map(this::convertirRespuesta)
        .toList();
    }

    @Transactional(readOnly = true)
    public EncuestaServicioResponse buscarPorId(Long id) {
        return convertirRespuesta(
            buscarEntidad(id)
        );
    }

    @Transactional(readOnly = true)
    public EncuestaServicioResponse buscarPorServicio(
            Long servicioId) {

        EncuestaServicio encuesta =
            encuestaRepository.findByServicioId(servicioId)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El servicio todavía no tiene una encuesta"
                    )
                );

        return convertirRespuesta(encuesta);
    }

    @Transactional
    public EncuestaServicioResponse registrar(
            Long servicioId,
            EncuestaServicioRequest request,
            String evaluadorNombre,
            String evaluadorCorreo) {

        ServicioTransporte servicio =
            servicioRepository.findById(servicioId)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio de transporte no encontrado"
                    )
                );

        if (servicio.getEstado()
                != EstadoServicio.COMPLETADO) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Solo se pueden evaluar servicios completados"
            );
        }

        if (encuestaRepository
                .existsByServicioId(servicioId)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El servicio ya cuenta con una encuesta registrada"
            );
        }

        EncuestaServicio encuesta =
            new EncuestaServicio();

        encuesta.setServicio(servicio);
        encuesta.setPuntualidad(request.puntualidad());
        encuesta.setCumplimiento(request.cumplimiento());
        encuesta.setAtencion(request.atencion());

        encuesta.setSatisfaccionGeneral(
            request.satisfaccionGeneral()
        );

        encuesta.setComentarios(
            limpiarTexto(request.comentarios())
        );

        encuesta.setEvaluadorNombre(
            limpiarNombre(evaluadorNombre)
        );

        encuesta.setEvaluadorCorreo(
            limpiarTexto(evaluadorCorreo)
        );

        encuesta.setTipoEvaluador(
            TipoEvaluador.INTERNO
        );

        return convertirRespuesta(
            encuestaRepository.save(encuesta)
        );
    }

    private EncuestaServicio buscarEntidad(Long id) {
        return encuestaRepository.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Encuesta de servicio no encontrada"
                )
            );
    }

    private EncuestaServicioResponse convertirRespuesta(
            EncuestaServicio encuesta) {

        ServicioTransporte servicio =
            encuesta.getServicio();

        return new EncuestaServicioResponse(
            encuesta.getId(),
            servicio.getId(),
            servicio.getCodigo(),
            encuesta.getPuntualidad(),
            encuesta.getCumplimiento(),
            encuesta.getAtencion(),
            encuesta.getSatisfaccionGeneral(),
            calcularPromedio(encuesta),
            encuesta.getComentarios(),
            encuesta.getEvaluadorNombre(),
            encuesta.getEvaluadorCorreo(),
            encuesta.getTipoEvaluador(),
            encuesta.getFechaRegistro()
        );
    }

    private BigDecimal calcularPromedio(
            EncuestaServicio encuesta) {

        int suma =
            encuesta.getPuntualidad()
            + encuesta.getCumplimiento()
            + encuesta.getAtencion()
            + encuesta.getSatisfaccionGeneral();

        return BigDecimal.valueOf(suma)
            .divide(
                BigDecimal.valueOf(4),
                2,
                RoundingMode.HALF_UP
            );
    }

    private String limpiarNombre(String valor) {
        String nombre = limpiarTexto(valor);

        if (nombre == null) {
            return "Usuario interno";
        }

        return nombre;
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();

        return limpio.isEmpty()
            ? null
            : limpio;
    }
}