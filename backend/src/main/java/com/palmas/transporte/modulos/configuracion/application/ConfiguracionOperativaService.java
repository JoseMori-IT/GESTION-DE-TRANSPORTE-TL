package com.palmas.transporte.modulos.configuracion.application;

import com.palmas.transporte.modulos.configuracion.api.BaseOperativa;
import com.palmas.transporte.modulos.configuracion.api.ConfiguracionOperativaRequest;
import com.palmas.transporte.modulos.configuracion.api.ConfiguracionOperativaResponse;
import com.palmas.transporte.modulos.configuracion.domain.ConfiguracionOperativa;
import com.palmas.transporte.modulos.configuracion.persistence.ConfiguracionOperativaRepository;
import com.palmas.transporte.modulos.recorridos.api.CatalogoUbicaciones;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Parámetros de la operación, separados de credenciales y configuración técnica. */
@Service
public class ConfiguracionOperativaService implements BaseOperativa {
    private static final long CONFIGURACION_UNICA = 1L;
    private final ConfiguracionOperativaRepository datos;
    private final CatalogoUbicaciones puntos;
    private final String sedeInicial;
    private final BigDecimal consumoInicial;
    private final BigDecimal emisionesInicial;
    private final BigDecimal litrosGalon;
    private final String fuenteConsumo;
    private final String fuenteEmisiones;

    public ConfiguracionOperativaService(
        ConfiguracionOperativaRepository datos,
        CatalogoUbicaciones puntos,
        @Value("${app.operacion.sede}") String sede,
        @Value("${app.operacion.galones-por-km}") BigDecimal consumo,
        @Value("${app.operacion.kg-co2-por-km}") BigDecimal emisiones,
        @Value("${app.operacion.litros-por-galon}") BigDecimal litros,
        @Value("${app.operacion.referencia-combustible}") String fuenteConsumo,
        @Value("${app.operacion.referencia-emisiones}") String fuenteEmisiones
    ) {
        this.datos = datos;
        this.puntos = puntos;
        this.sedeInicial = sede;
        this.consumoInicial = consumo;
        this.emisionesInicial = emisiones;
        this.litrosGalon = litros;
        this.fuenteConsumo = fuenteConsumo;
        this.fuenteEmisiones = fuenteEmisiones;
        var inicial = new ConfiguracionOperativaRequest(null, sede, null, consumo, emisiones, fuenteConsumo, fuenteEmisiones);
        // The same constraints apply to defaults supplied through environment variables.
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            if (!factory.getValidator().validate(inicial).isEmpty() || litros.signum() <= 0) {
                throw new IllegalStateException("Revisa los parámetros operativos iniciales del servidor.");
            }
        }
    }

    @Transactional(readOnly = true)
    public ConfiguracionOperativaResponse consultar() {
        return datos.findById(CONFIGURACION_UNICA).map(this::respuesta).orElseGet(() ->
            new ConfiguracionOperativaResponse(null, sedeInicial, null, null,
                consumoInicial, emisionesInicial, litrosGalon, "Galón estadounidense",
                fuenteConsumo, fuenteEmisiones, null, null));
    }

    @Transactional
    public ConfiguracionOperativaResponse guardar(ConfiguracionOperativaRequest solicitud, String actor) {
        var configuracion = datos.findById(CONFIGURACION_UNICA).orElseGet(ConfiguracionOperativa::new);
        if (!Objects.equals(configuracion.getVersionRegistro(), solicitud.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Los parámetros cambiaron. Actualiza antes de guardar.");
        }
        if (solicitud.plantaBaseId() != null && !puntos.consultar(solicitud.plantaBaseId()).plantaActiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Selecciona una ubicación activa de tipo PLANTA.");
        }
        configuracion.setId(CONFIGURACION_UNICA);
        configuracion.setSede(solicitud.sede().trim());
        configuracion.setPlantaBaseId(solicitud.plantaBaseId());
        configuracion.setGalonesPorKm(solicitud.galonesPorKm());
        configuracion.setKgCo2PorKm(solicitud.kgCo2PorKm());
        configuracion.setReferenciaCombustible(solicitud.referenciaCombustible().trim());
        configuracion.setReferenciaEmisiones(solicitud.referenciaEmisiones().trim());
        configuracion.setActualizadoPor(actor);
        configuracion.setActualizadoEn(LocalDateTime.now());
        return respuesta(datos.saveAndFlush(configuracion));
    }

    private ConfiguracionOperativaResponse respuesta(ConfiguracionOperativa configuracion) {
        var plantaId = configuracion.getPlantaBaseId();
        return new ConfiguracionOperativaResponse(configuracion.getVersionRegistro(),
            configuracion.getSede(), plantaId, plantaId == null ? null : puntos.consultar(plantaId).nombre(),
            configuracion.getGalonesPorKm(), configuracion.getKgCo2PorKm(), litrosGalon,
            "Galón estadounidense", configuracion.getReferenciaCombustible(),
            configuracion.getReferenciaEmisiones(), configuracion.getActualizadoPor(), configuracion.getActualizadoEn());
    }

    @Override
    @Transactional(readOnly = true)
    public void validarPlanta(Long inicioId, Long retornoId) {
        var configuracion = consultar();
        if (configuracion.plantaBaseId() == null) return;
        if (!puntos.consultar(configuracion.plantaBaseId()).plantaActiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La planta base debe estar activa. Revisa los parámetros operativos.");
        }
        if (!configuracion.plantaBaseId().equals(inicioId) || !configuracion.plantaBaseId().equals(retornoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El viaje debe salir y regresar a la planta base configurada: " + configuracion.plantaBaseNombre());
        }
    }
}
