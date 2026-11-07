import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoSolicitudTransporte =
  'RECOJO_RFF' | 'RECOJO_FS' | 'MOVIMIENTO_INTERNO' | 'DISPOSICION_SUBPRODUCTO' | 'OTRO';

export type PrioridadSolicitud = 'BAJA' | 'NORMAL' | 'ALTA' | 'URGENTE';

export type EstadoSolicitud =
  | 'SOLICITADA'
  | 'EN_REVISION'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'PROGRAMADA'
  | 'EN_CURSO'
  | 'COMPLETADA'
  | 'CANCELADA'
  | 'INTERRUMPIDA';

export type ProductoSolicitud =
  | 'RFF'
  | 'FS'
  | 'ESCOBAJO'
  | 'FIBRA'
  | 'TORTA_CLARIFICACION'
  | 'LODO_BIOGAS'
  | 'CENIZA'
  | 'RESIDUOS_VARIOS';

export interface SolicitudTransporte {
  rutaId: number | null;
  planViaje?: import('./servicio-transporte.service').PlanViajeRequest | null;
  resumenRecorrido?: string | null;
  distanciaEstimadaKm?: number | null;
  duracionEstimadaMinutos?: number | null;
  id: number;
  codigo: string;
  tipoSolicitud: TipoSolicitudTransporte;
  solicitanteNombre: string;
  areaSolicitante: string;
  telefono: string | null;
  origen: string;
  destino: string;
  productoTransportado: ProductoSolicitud;
  descripcionProducto: string | null;
  cantidadEstimadaToneladas: number | null;
  fechaRequerida: string;
  prioridad: PrioridadSolicitud;
  estado: EstadoSolicitud;
  observaciones: string | null;
  motivoRechazoCancelacion: string | null;
  usuarioSolicitanteId: number;
  usuarioSolicitanteCorreo: string;
  servicioId: number | null;
  servicioCodigo: string | null;
  fechaRegistro: string;
  fechaActualizacion: string | null;
}

export interface RegistrarSolicitudRequest {
  planViaje: import('./servicio-transporte.service').PlanViajeRequest;
  rutaId: number;
  tipoSolicitud: TipoSolicitudTransporte;
  solicitanteNombre: string;
  areaSolicitante: string;
  telefono: string | null;
  origen: string;
  destino: string;
  productoTransportado: ProductoSolicitud;
  descripcionProducto: string | null;
  cantidadEstimadaToneladas: number | null;
  fechaRequerida: string;
  prioridad: PrioridadSolicitud;
  observaciones: string | null;
}

export interface ProgramarSolicitudRequest {
  tramos: import('./servicio-transporte.service').TramoViajeRequest[];
  planViaje?: import('./servicio-transporte.service').PlanViajeRequest;
  codigoServicio: string;
  vehiculoId: number;
  conductorId: number;
  copilotoId: number | null;
  rutaId: number;
  requiereRetorno: boolean;
  fechaHoraSalida: string | null;
  observaciones: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class SolicitudTransporteService {
  private readonly http = inject(HttpClient);

  private readonly url = '/api/solicitudes';

  listar(): Observable<SolicitudTransporte[]> {
    return this.http.get<SolicitudTransporte[]>(this.url);
  }

  buscarPorId(id: number): Observable<SolicitudTransporte> {
    return this.http.get<SolicitudTransporte>(`${this.url}/${id}`);
  }

  registrar(request: RegistrarSolicitudRequest): Observable<SolicitudTransporte> {
    return this.http.post<SolicitudTransporte>(this.url, request);
  }

  cambiarEstado(
    id: number,
    estado: EstadoSolicitud,
    motivo: string | null = null,
  ): Observable<SolicitudTransporte> {
    return this.http.patch<SolicitudTransporte>(`${this.url}/${id}/estado`, {
      estado,
      motivo,
    });
  }

  completarRecorrido(
    id: number,
    rutaId: number,
    planViaje: import('./servicio-transporte.service').PlanViajeRequest,
  ): Observable<SolicitudTransporte> {
    return this.http.patch<SolicitudTransporte>(`${this.url}/${id}/recorrido`, {
      rutaId,
      planViaje,
    });
  }
  programar(id: number, request: ProgramarSolicitudRequest): Observable<SolicitudTransporte> {
    return this.http.post<SolicitudTransporte>(`${this.url}/${id}/programar`, request);
  }
}
