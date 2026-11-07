package com.palmas.transporte.modulos.recorridos.application;
import com.palmas.transporte.modulos.recorridos.api.CatalogoUbicaciones;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service public class ConsultaCatalogoUbicaciones implements CatalogoUbicaciones {
 private final UbicacionRepository datos;
 public ConsultaCatalogoUbicaciones(UbicacionRepository datos){this.datos=datos;}
 public Punto consultar(Long id){var u=datos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"La planta configurada ya no existe. Revisa los parámetros operativos."));return new Punto(u.getId(),u.getNombre(),u.getTipo()==TipoUbicacion.PLANTA&&u.getEstado()==EstadoRuta.ACTIVA);}
}
