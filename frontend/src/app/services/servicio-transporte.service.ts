import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type EstadoServicio =
  'PROGRAMADO' | 'EN_CURSO' | 'COMPLETADO' | 'CANCELADO' | 'INTERRUMPIDO';

export type ProductoTransportado =
  | 'RFF'
  | 'FS'
  | 'ESCOBAJO'
  | 'FIBRA'
  | 'TORTA_CLARIFICACION'
  | 'LODO_BIOGAS'
  | 'CENIZA'
  | 'RESIDUOS_VARIOS';

export interface PersonaServicio {
  id: number;
  nombres: string;
  apellidos: string;
  numeroDocumento: string | null;

  tipoDocumento: 'DNI' | 'CARNET_EXTRANJERIA' | 'PASAPORTE' | 'OTRO';

  numeroLicencia: string | null;
  categoriaLicencia: string | null;
  vencimientoLicencia: string | null;

  estado: string;
  rolTripulacion: string;
}

export interface VehiculoServicio {
  odometroKm?: number | null;
  estadoOdometro?: 'OPERATIVO' | 'AVERIADO' | null;
  fechaOdometro?: string | null;

  id: number;
  codigoInterno: string;
  placa: string;
  marca: string;
  modelo: string;
  tipo: string;
  estado: string;
}

export interface RutaServicio {
  distanciaKm?: number;
  duracionEstimadaMinutos?: number;
  id: number;
  codigo: string;
  nombre: string;
  origen: string;
  destino: string;
  estado: string;
  origenUbicacion?: {
    id: number;
    nombre: string;
    latitud: number | null;
    longitud: number | null;
  } | null;
  destinoUbicacion?: {
    id: number;
    nombre: string;
    latitud: number | null;
    longitud: number | null;
  } | null;
}

export interface EstimacionTramoViaje {
  distanciaKm: number;
  duracionMinutos: number;
  fuente: 'CARRETERA' | 'MANUAL';
  recorrido: [number, number][];
}
export interface PlanViajeRequest {
  inicioUbicacionId: number;
  plantaRetornoId: number;
  paradas: {
    ubicacionId: number;
    actividad: 'RECOJO' | 'ENTREGA' | 'PASO';
    producto: string;
    indicaciones: string;
  }[];
  estimaciones?: EstimacionTramoViaje[];
}
export interface TramoViajeRequest {
  rutaId: number;
  productoTransportado: string;
  actividad: 'RECOJO' | 'ENTREGA' | 'RETORNO' | 'PASO';
  indicaciones: string;
}
export interface TramoServicio {
  odometroNoDisponibleSalida?: boolean | null;
  odometroNoDisponibleLlegada?: boolean | null;
  motivoOdometroSalida?: string | null;
  motivoOdometroLlegada?: string | null;

  odometroSalidaKm?: number | null;
  odometroLlegadaKm?: number | null;
  id: number;
  orden: number;
  ruta: RutaServicio | null;
  origen: string;
  destino: string;
  productoTransportado: string;
  actividad: string;
  indicaciones: string | null;
  salidaReal: string | null;
  llegadaReal: string | null;
  ultimaLatitud?: number | null;
  ultimaLongitud?: number | null;
  origenUbicacion?: import('./ruta.service').Ubicacion | null;
  destinoUbicacion?: import('./ruta.service').Ubicacion | null;
  distanciaEstimadaKm?: number | null;
  duracionEstimadaMinutos?: number | null;
  fuenteDistancia?: 'CARRETERA' | 'MANUAL' | null;
  recorridoJson?: string | null;
}
export interface IncidenciaViaje {
  id: number;
  versionRegistro: number;
  estado: string;
  historial?: string;
  descripcion: string;
  lugar: string;
  latitud: number | null;
  longitud: number | null;
  fechaReporte: string;
  fechaAtencion: string | null;
  fechaReparacion: string | null;
  fechaResolucion: string | null;
  tecnico: string | null;
  diagnostico: string | null;
  servicioAuxilioId: number | null;
  mantenimientoId: number | null;
  odometroRegresoKm: number | null;
}
export interface ServicioTransporte {
  incidencias?: IncidenciaViaje[];
  detenidoPorAveria?: boolean;
  servicioAuxiliadoId?: number | null;

  tramos?: TramoServicio[];
  id: number;
  codigo: string;
  vehiculo: VehiculoServicio;
  conductor: PersonaServicio;
  copiloto: PersonaServicio | null;
  ruta: RutaServicio | null;
  origenDescripcion: string;
  destinoDescripcion: string;
  requiereRetorno: boolean;
  fechaHoraSalida: string | null;
  productoTransportado: string;
  descripcionResiduos: string | null;
  estado: EstadoServicio;
  observaciones: string | null;
  fechaRegistro: string;
  fechaHoraSalidaReal: string | null;
  fechaHoraLlegadaReal: string | null;
  origenLatitud?: number | null;
  origenLongitud?: number | null;
  destinoLatitud?: number | null;
  destinoLongitud?: number | null;
  distanciaEstimadaKm?: number | null;
  duracionEstimadaMinutos?: number | null;
}

export interface RegistrarServicioRequest {
  codigo: string;
  vehiculoId: number;
  conductorId: number;
  copilotoId: number | null;
  rutaId: number | null;
  origenDescripcion: string;
  destinoDescripcion: string;
  requiereRetorno: boolean;
  fechaHoraSalida: string | null;
  productoTransportado: ProductoTransportado;
  descripcionResiduos: string | null;
  estado: 'PROGRAMADO';
  observaciones: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class ServicioTransporteService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/servicios';

  listar(): Observable<ServicioTransporte[]> {
    return this.http.get<ServicioTransporte[]>(this.url);
  }

  registrar(datos: RegistrarServicioRequest): Observable<ServicioTransporte> {
    return this.http.post<ServicioTransporte>(this.url, datos);
  }

  cambiarEstado(id: number, estado: EstadoServicio): Observable<ServicioTransporte> {
    return this.http.patch<ServicioTransporte>(`${this.url}/${id}/estado`, { estado });
  }
}
