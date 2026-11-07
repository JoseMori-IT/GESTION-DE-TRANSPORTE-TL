package com.palmas.transporte.modulos.operacion.application;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TramoServicio;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;

import java.util.*;
import java.util.function.Function;
import com.palmas.transporte.modulos.operacion.api.TramoRequest;


public final class ItinerarioViaje {
    private ItinerarioViaje() {}
    public static void prepararPlan(ServicioTransporte servicio, com.palmas.transporte.modulos.operacion.api.PlanViajeRequest plan, Function<Long, Ubicacion> buscarPunto, List<Ruta> catalogo) {
        if (plan.inicioUbicacionId() == null || plan.plantaRetornoId() == null || plan.paradas() == null || plan.paradas().isEmpty() || plan.paradas().size() > 29) throw new IllegalArgumentException("Define inicio, al menos una parada y planta de retorno.");
        Ubicacion inicio = puntoActivo(buscarPunto.apply(plan.inicioUbicacionId()));
        Ubicacion planta = puntoActivo(buscarPunto.apply(plan.plantaRetornoId()));
        if (!esBase(planta)) throw new IllegalArgumentException("El viaje debe terminar en una ubicación de tipo PLANTA.");
        if (!Objects.equals(inicio.getId(), planta.getId())) throw new IllegalArgumentException("El regreso debe ser al mismo origen del viaje.");
        List<Ubicacion> puntos = new ArrayList<>(); puntos.add(inicio);
        for (var parada : plan.paradas()) puntos.add(puntoActivo(buscarPunto.apply(parada.ubicacionId())));
        puntos.add(planta);
        for (int i=1; i<puntos.size(); i++) if (Objects.equals(puntos.get(i-1).getId(), puntos.get(i).getId())) throw new IllegalArgumentException("Dos puntos consecutivos no pueden ser la misma ubicación.");
        // El plan debe atender la ruta de la solicitud, aunque primero vaya vacío a recoger la carga.
        Ruta solicitada = servicio.getRuta();
        boolean atiende = false;
        for (int i=0; i<puntos.size(); i++) {
            for (int j=i+1; j<puntos.size(); j++) if (Objects.equals(puntos.get(i).getId(),solicitada.getOrigenUbicacion().getId()) && Objects.equals(puntos.get(j).getId(),solicitada.getDestinoUbicacion().getId())) atiende=true;
        }
        if (!atiende) throw new IllegalArgumentException("El recorrido debe incluir el origen y destino de la solicitud en ese orden.");
        String cargaRetorno = "SIN_CARGA";
        for (int i=1; i<puntos.size(); i++) {
            Ubicacion origen=puntos.get(i-1), destino=puntos.get(i);
            TramoServicio tramo = new TramoServicio(); tramo.setServicio(servicio); tramo.setOrden(i);
            tramo.setOrigenUbicacion(origen); tramo.setDestinoUbicacion(destino); tramo.setOrigen(origen.getNombre()); tramo.setDestino(destino.getNombre());
            tramo.setRuta(catalogo.stream().filter(r -> r.getEstado()==EstadoRuta.ACTIVA && r.getOrigenUbicacion()!=null && r.getDestinoUbicacion()!=null && Objects.equals(r.getOrigenUbicacion().getId(),origen.getId()) && Objects.equals(r.getDestinoUbicacion().getId(),destino.getId())).sorted(Comparator.comparing(Ruta::getId)).findFirst().orElse(null));
            if (i<=plan.paradas().size()) {
                var parada=plan.paradas().get(i-1); tramo.setActividad(parada.actividad()); tramo.setProductoTransportado(parada.producto()); tramo.setIndicaciones(parada.indicaciones());
                if ("RECOJO".equals(parada.actividad())) cargaRetorno=parada.producto();
                if ("ENTREGA".equals(parada.actividad())) cargaRetorno="SIN_CARGA";
            } else { tramo.setActividad("RETORNO"); tramo.setProductoTransportado(cargaRetorno); }
            servicio.getTramos().add(tramo);
        }
        servicio.setOrigenDescripcion(inicio.getNombre()); servicio.setOrigenLatitud(inicio.getLatitud()); servicio.setOrigenLongitud(inicio.getLongitud());
        servicio.setDestinoDescripcion(planta.getNombre()); servicio.setDestinoLatitud(planta.getLatitud()); servicio.setDestinoLongitud(planta.getLongitud()); servicio.setRequiereRetorno(true);
        boolean conocidas=servicio.getTramos().stream().allMatch(t -> t.getRuta()!=null);
        servicio.setDistanciaEstimadaKm(conocidas ? servicio.getTramos().stream().map(t -> t.getRuta().getDistanciaKm()).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add) : null);
        servicio.setDuracionEstimadaMinutos(conocidas ? servicio.getTramos().stream().mapToInt(t -> t.getRuta().getDuracionEstimadaMinutos()).sum() : null);
        if (plan.estimaciones()!=null) {
            if (plan.estimaciones().size()!=servicio.getTramos().size()) throw new IllegalArgumentException("Completa las distancias de todos los tramos, incluido el retorno.");
            for (int i=0;i<servicio.getTramos().size();i++) {
                var estimacion=plan.estimaciones().get(i); var tramo=servicio.getTramos().get(i);
                if (estimacion==null || estimacion.distanciaKm()==null || estimacion.distanciaKm().signum()<=0 || estimacion.duracionMinutos()==null || estimacion.duracionMinutos()<1 || !estimacion.isRecorridoValido() || estimacion.fuente()==null || !Set.of("CARRETERA","MANUAL").contains(estimacion.fuente())) throw new IllegalArgumentException("Revisa las estimaciones y los trazados de cada tramo.");
                tramo.setDistanciaEstimadaKm(estimacion.distanciaKm());tramo.setDuracionEstimadaMinutos(estimacion.duracionMinutos());tramo.setFuenteDistancia(estimacion.fuente());tramo.setRecorridoJson(estimacion.recorridoJson());
            }
            servicio.setDistanciaEstimadaKm(servicio.getTramos().stream().map(TramoServicio::getDistanciaEstimadaKm).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add));
            servicio.setDuracionEstimadaMinutos(servicio.getTramos().stream().mapToInt(TramoServicio::getDuracionEstimadaMinutos).sum());
        }
    }
    public static boolean esBase(Ubicacion u) {return u!=null && u.getTipo()==TipoUbicacion.PLANTA;}
    private static Ubicacion puntoActivo(Ubicacion punto) {
        if (punto==null || punto.getEstado()!=EstadoRuta.ACTIVA) throw new IllegalArgumentException("Todos los puntos del viaje deben existir y estar activos.");
        return punto;
    }
    public static void preparar(ServicioTransporte servicio, List<TramoRequest> solicitudes, Function<Long, Ruta> buscarRuta) {
        List<TramoRequest> lista = solicitudes == null || solicitudes.isEmpty()
            ? List.of(new TramoRequest(servicio.getRuta().getId(), servicio.getProductoTransportado(), "ENTREGA", null)) : solicitudes;
        if (lista.size() > 30) throw new IllegalArgumentException("Un viaje admite hasta 30 tramos.");
        if (!Objects.equals(lista.get(0).rutaId(), servicio.getRuta().getId())) throw new IllegalArgumentException("El primer tramo debe ser la ruta de la solicitud.");
        if (lista.size() > 1 && Boolean.TRUE.equals(servicio.getRequiereRetorno())) throw new IllegalArgumentException("En viajes con paradas agrega el retorno como último tramo del itinerario.");
        Ruta anterior = null;
        for (TramoRequest datos : lista) {
            Ruta ruta = buscarRuta.apply(datos.rutaId());
            if (anterior != null && !Objects.equals(anterior.getDestinoUbicacion().getId(), ruta.getOrigenUbicacion().getId())) throw new IllegalArgumentException("Cada tramo debe comenzar donde termina el anterior.");
            TramoServicio tramo = new TramoServicio();
            tramo.setServicio(servicio); tramo.setOrden(servicio.getTramos().size() + 1); tramo.setRuta(ruta);
            tramo.setOrigen(ruta.getOrigen()); tramo.setDestino(ruta.getDestino());
            tramo.setProductoTransportado(datos.productoTransportado()); tramo.setActividad(datos.actividad()); tramo.setIndicaciones(datos.indicaciones());
            servicio.getTramos().add(tramo); anterior = ruta;
        }
        // Compatibilidad con el retorno simple anterior; en itinerarios nuevos se elige una ruta de retorno explícita.
        if (Boolean.TRUE.equals(servicio.getRequiereRetorno())) {
            TramoServicio retorno = new TramoServicio(); retorno.setServicio(servicio); retorno.setOrden(2); retorno.setRuta(servicio.getRuta());
            retorno.setOrigen(servicio.getRuta().getDestino()); retorno.setDestino(servicio.getRuta().getOrigen());
            retorno.setProductoTransportado("SIN_CARGA"); retorno.setActividad("RETORNO"); servicio.getTramos().add(retorno);
        }
        servicio.setDestinoDescripcion(servicio.getTramos().get(servicio.getTramos().size() - 1).getDestino());
        Ubicacion destinoFinal = Boolean.TRUE.equals(servicio.getRequiereRetorno()) ? servicio.getRuta().getOrigenUbicacion() : anterior.getDestinoUbicacion();
        servicio.setDestinoLatitud(destinoFinal.getLatitud()); servicio.setDestinoLongitud(destinoFinal.getLongitud());
        servicio.setRequiereRetorno(servicio.getTramos().size() > 1 && Objects.equals(destinoFinal.getId(), servicio.getRuta().getOrigenUbicacion().getId()));
        servicio.setDistanciaEstimadaKm(servicio.getTramos().stream().map(t -> t.getRuta().getDistanciaKm()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        servicio.setDuracionEstimadaMinutos(servicio.getTramos().stream().mapToInt(t -> t.getRuta().getDuracionEstimadaMinutos()).sum());
    }
}
