package com.palmas.transporte.modulos.auditoria.application;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmas.transporte.modulos.auditoria.api.AuditoriaResponse;
import com.palmas.transporte.modulos.auditoria.domain.Auditoria;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.auditoria.persistence.AuditoriaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditoriaConsultaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaConsultaService(
            AuditoriaRepository auditoriaRepository) {

        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> consultar(
            String texto,
            String usuarioCorreo,
            String accion,
            String modulo,
            ResultadoAuditoria resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int pagina,
            int tamanio) {

        int paginaSegura = Math.max(pagina, 0);

        int tamanioSeguro = Math.min(
            Math.max(tamanio, 1),
            100
        );

        Pageable pageable = PageRequest.of(
            paginaSegura,
            tamanioSeguro,
            Sort.by(
                Sort.Order.desc("fechaRegistro"),
                Sort.Order.desc("id")
            )
        );

        Specification<Auditoria> filtros =
            crearFiltros(
                texto,
                usuarioCorreo,
                accion,
                modulo,
                resultado,
                fechaDesde,
                fechaHasta
            );

        return auditoriaRepository
            .findAll(filtros, pageable)
            .map(this::convertirRespuesta);
    }

    private Specification<Auditoria> crearFiltros(
            String texto,
            String usuarioCorreo,
            String accion,
            String modulo,
            ResultadoAuditoria resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (raiz, consulta, constructor) -> {
            List<Predicate> condiciones =
                new ArrayList<>();

            condiciones.add(constructor.upper(raiz.<String>get("modulo")).in("AUTENTICACION"));
            condiciones.add(constructor.upper(raiz.<String>get("accion")).in("INICIAR_SESION", "CERRAR_SESION"));

            String textoLimpio = limpiar(texto);

            if (textoLimpio != null) {
                String patron =
                    "%" + textoLimpio.toLowerCase() + "%";

                condiciones.add(
                    constructor.or(
                        constructor.like(
                            constructor.lower(
                                raiz.get("usuarioNombre")
                            ),
                            patron
                        ),
                        constructor.like(
                            constructor.lower(
                                raiz.get("usuarioCorreo")
                            ),
                            patron
                        ),
                        constructor.like(
                            constructor.lower(
                                raiz.get("codigoReferencia")
                            ),
                            patron
                        ),
                        constructor.like(
                            constructor.lower(
                                raiz.get("detalle")
                            ),
                            patron
                        ),
                        constructor.like(
                            constructor.lower(
                                raiz.get("entidadId")
                            ),
                            patron
                        )
                    )
                );
            }

            String correoLimpio =
                limpiar(usuarioCorreo);

            if (correoLimpio != null) {
                condiciones.add(
                    constructor.equal(
                        constructor.lower(
                            raiz.get("usuarioCorreo")
                        ),
                        correoLimpio.toLowerCase()
                    )
                );
            }

            String accionLimpia = limpiar(accion);

            if (accionLimpia != null) {
                condiciones.add(
                    constructor.equal(
                        constructor.upper(
                            raiz.get("accion")
                        ),
                        accionLimpia.toUpperCase()
                    )
                );
            }

            String moduloLimpio = limpiar(modulo);

            if (moduloLimpio != null) {
                condiciones.add(
                    constructor.equal(
                        constructor.upper(
                            raiz.get("modulo")
                        ),
                        moduloLimpio.toUpperCase()
                    )
                );
            }

            if (resultado != null) {
                condiciones.add(
                    constructor.equal(
                        raiz.get("resultado"),
                        resultado
                    )
                );
            }

            if (fechaDesde != null) {
                LocalDateTime inicio =
                    fechaDesde.atStartOfDay();

                condiciones.add(
                    constructor.greaterThanOrEqualTo(
                        raiz.get("fechaRegistro"),
                        inicio
                    )
                );
            }

            if (fechaHasta != null) {
                LocalDateTime limiteExclusivo =
                    fechaHasta
                        .plusDays(1)
                        .atStartOfDay();

                condiciones.add(
                    constructor.lessThan(
                        raiz.get("fechaRegistro"),
                        limiteExclusivo
                    )
                );
            }

            return constructor.and(
                condiciones.toArray(
                    new Predicate[0]
                )
            );
        };
    }

    private AuditoriaResponse convertirRespuesta(
            Auditoria auditoria) {

        return new AuditoriaResponse(
            auditoria.getId(),

            auditoria.getUsuarioId(),
            auditoria.getUsuarioNombre(),
            auditoria.getUsuarioCorreo(),
            auditoria.getUsuarioRol(),

            auditoria.getAccion(),
            auditoria.getModulo(),

            auditoria.getEntidad(),
            auditoria.getEntidadId(),
            auditoria.getCodigoReferencia(),

            auditoria.getDetalle(),
            auditoria.getValorAnterior(),
            auditoria.getValorNuevo(),

            auditoria.getResultado(),

            auditoria.getDireccionIp(),
            auditoria.getUserAgent(),

            auditoria.getFechaRegistro()
        );
    }

    private String limpiar(String valor) {
        if (
            valor == null
            || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }
}
