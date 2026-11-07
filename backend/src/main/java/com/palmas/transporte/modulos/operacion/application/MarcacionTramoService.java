package com.palmas.transporte.modulos.operacion.application;
import com.palmas.transporte.modulos.flota.application.OdometroService;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioResponse;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EventoTramo;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.operacion.persistence.EventoTramoRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.*;




@Service
public class MarcacionTramoService {
    private final EventoTramoRepository eventos;
    private final ServicioTransporteRepository servicios;
    private final ServicioTransporteService servicioService;
    private final EntityManager em;
    private final OdometroService odometros;
    private final IncidenciaViajeService incidencias;
    public MarcacionTramoService(EventoTramoRepository eventos, ServicioTransporteRepository servicios, ServicioTransporteService servicioService, EntityManager em, OdometroService odometros, IncidenciaViajeService incidencias) {
        this.eventos = eventos; this.servicios = servicios; this.servicioService = servicioService; this.em = em; this.odometros=odometros;this.incidencias=incidencias;
    }
    @Transactional(readOnly = true)
    public List<MarcacionServicioResponse> listar(Long id) {
        return eventos.findByTramoServicioIdOrderByFechaHoraDispositivoAsc(id).stream().map(this::respuesta).toList();
    }
    @Transactional
    public MarcacionServicioResponse registrar(Long id, Long conductorId, MarcacionServicioRequest r) {
        ServicioTransporte servicio = servicios.findByIdAndConductorId(id, conductorId).orElseThrow(() -> error("El viaje no pertenece al chofer autenticado."));
        em.refresh(servicio, LockModeType.PESSIMISTIC_WRITE);
        var repetido = eventos.findByEventoUuid(r.eventoUuid());
        if (repetido.isPresent()) {
            var previo = repetido.get();
            if (!previo.getTramo().getServicio().getId().equals(id) || !previo.getTramo().getOrden().equals(r.tramoOrden()) || previo.getTipo() != r.tipo()) throw error("El identificador ya fue utilizado en otra marcación.");
            return respuesta(previo);
        }
        if(servicio.isDetenidoPorAveria() || servicio.getEstado()==EstadoServicio.INTERRUMPIDO) throw error("El viaje está detenido por avería o fue interrumpido. Atiende la incidencia antes de continuar.");
        TramoServicio tramo = servicio.getTramos().stream().filter(t -> t.getLlegadaReal() == null).findFirst().orElseThrow(() -> error("El viaje ya finalizó."));
        if (!tramo.getOrden().equals(r.tramoOrden()) || servicio.getEstado() == EstadoServicio.CANCELADO || servicio.getEstado() == EstadoServicio.COMPLETADO) throw error("La parada no corresponde al avance actual. Actualiza el viaje.");
        boolean inicioViaje = tramo.getOrden()==1 && r.tipo()==TipoMarcacionServicio.SALIDA_IDA;
        boolean finViaje = tramo.getOrden()==servicio.getTramos().size() && r.tipo()==TipoMarcacionServicio.LLEGADA_DESTINO;
        em.refresh(servicio.getVehiculo(), LockModeType.PESSIMISTIC_WRITE);
        odometros.validarMarcacion(servicio,r,inicioViaje,finViaje);
        LocalDateTime anterior = tramo.getOrden() > 1 ? servicio.getTramos().get(tramo.getOrden() - 2).getLlegadaReal() : null;
        if (r.tipo() == TipoMarcacionServicio.SALIDA_IDA) {
            // Compatibilidad con marcaciones de regreso guardadas por versiones anteriores.
            boolean retornoAuxilioAnterior=servicio.getServicioAuxiliadoId()!=null && tramo.getOrden()==2 && Objects.equals(tramo.getSalidaReal(),anterior);
            if ((tramo.getSalidaReal() != null && !retornoAuxilioAnterior) || (anterior != null && r.fechaHoraDispositivo().isBefore(anterior))) throw error("La salida ya fue registrada o su hora es anterior a la llegada previa.");
            if (tramo.getOrden() == 1) {
                if (servicio.getEstado() == EstadoServicio.PROGRAMADO) servicioService.cambiarEstadoDesdeMarcacion(id, EstadoServicio.EN_CURSO);
                else if (servicio.getEstado() != EstadoServicio.EN_CURSO) throw error("El viaje no está programado ni en curso.");
                servicio.setFechaHoraSalidaReal(r.fechaHoraDispositivo());
            } else if (servicio.getEstado() != EstadoServicio.EN_CURSO) throw error("El viaje no está en curso.");
            tramo.setSalidaReal(r.fechaHoraDispositivo());
        } else if (r.tipo() == TipoMarcacionServicio.LLEGADA_DESTINO) {
            if (tramo.getSalidaReal() == null || r.fechaHoraDispositivo().isBefore(tramo.getSalidaReal()) || servicio.getEstado() != EstadoServicio.EN_CURSO) throw error("Primero registra la salida del tramo y revisa la hora del dispositivo.");
            tramo.setLlegadaReal(r.fechaHoraDispositivo());
            if(servicio.getServicioAuxiliadoId()!=null && tramo.getOrden()==1 && servicio.getTramos().size()==2){
                var retorno=servicio.getTramos().get(1);
                if(retorno.getSalidaReal()==null)retorno.setSalidaReal(r.fechaHoraDispositivo());
            }
        } else throw error("Para cada tramo solo se admite salida y llegada.");
        if (inicioViaje) {tramo.setOdometroSalidaKm(r.odometroKm());tramo.setOdometroNoDisponibleSalida(Boolean.TRUE.equals(r.odometroNoDisponible()));tramo.setMotivoOdometroSalida(r.motivoOdometro());}
        if (finViaje) {tramo.setOdometroLlegadaKm(r.odometroKm());tramo.setOdometroNoDisponibleLlegada(Boolean.TRUE.equals(r.odometroNoDisponible()));tramo.setMotivoOdometroLlegada(r.motivoOdometro());}
        tramo.setUltimaLatitud(r.latitud()); tramo.setUltimaLongitud(r.longitud());
        servicios.saveAndFlush(servicio);
        if (servicio.getTramos().stream().allMatch(t -> t.getLlegadaReal() != null)) {
            servicioService.cambiarEstadoDesdeMarcacion(id, EstadoServicio.COMPLETADO);
            servicio.setFechaHoraLlegadaReal(r.fechaHoraDispositivo()); servicios.saveAndFlush(servicio);
            incidencias.completarRetornoAuxilio(servicio,r.fechaHoraDispositivo(),"Chofer #"+conductorId);
        }
        odometros.registrarMarcacion(servicio,r,inicioViaje,finViaje);
        EventoTramo evento = new EventoTramo(); evento.setTramo(tramo); evento.setEventoUuid(r.eventoUuid()); evento.setTipo(r.tipo());
        evento.setLatitud(r.latitud()); evento.setLongitud(r.longitud()); evento.setPrecisionMetros(r.precisionMetros());
        evento.setFechaHoraDispositivo(r.fechaHoraDispositivo()); evento.setFechaHoraServidor(LocalDateTime.now()); evento.setRegistradoOffline(r.registradoOffline()); evento.setObservacion(r.observacion());
        return respuesta(eventos.save(evento));
    }
    private ResponseStatusException error(String mensaje) { return new ResponseStatusException(HttpStatus.CONFLICT, mensaje); }
    private MarcacionServicioResponse respuesta(EventoTramo e) {
        return new MarcacionServicioResponse(e.getId(), e.getEventoUuid(), e.getTramo().getServicio().getId(), e.getTipo(), e.getLatitud(), e.getLongitud(), e.getPrecisionMetros(), e.getFechaHoraDispositivo(), e.getFechaHoraServidor(), e.getRegistradoOffline(), e.getObservacion(), e.getTramo().getOrden(),
            e.getTipo()==TipoMarcacionServicio.SALIDA_IDA?e.getTramo().getOdometroSalidaKm():e.getTramo().getOdometroLlegadaKm(),
            e.getTipo()==TipoMarcacionServicio.SALIDA_IDA?e.getTramo().getOdometroNoDisponibleSalida():e.getTramo().getOdometroNoDisponibleLlegada(),
            e.getTipo()==TipoMarcacionServicio.SALIDA_IDA?e.getTramo().getMotivoOdometroSalida():e.getTramo().getMotivoOdometroLlegada());
    }
}
