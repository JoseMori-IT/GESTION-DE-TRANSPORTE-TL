import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoFalla =
  | 'MOTOR'
  | 'TRANSMISION'
  | 'FRENOS'
  | 'SUSPENSION'
  | 'DIRECCION'
  | 'SISTEMA_ELECTRICO'
  | 'NEUMATICOS'
  | 'CARROCERIA'
  | 'AIRE_ACONDICIONADO'
  | 'MANTENIMIENTO_PREVENTIVO'
  | 'DOCUMENTACION'
  | 'OTRO';

export type PrioridadMantenimiento = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export type EstadoMantenimiento =
  'REPORTADO' | 'EN_EVALUACION' | 'EN_MANTENIMIENTO' | 'REPARADO' | 'CERRADO' | 'CANCELADO';

export type TipoEvidenciaMantenimiento =
  'FALLA_REPORTADA' | 'DIAGNOSTICO' | 'REPARACION' | 'CONFORMIDAD';

export interface Mantenimiento {
  id: number;
  codigo: string;

  vehiculoId: number;
  vehiculoCodigo: string;
  vehiculoPlaca: string;
  vehiculoDescripcion: string;

  reportadoPorId: number;
  reportadoPorNombre: string;

  tipoFalla: TipoFalla;
  prioridad: PrioridadMantenimiento;
  estado: EstadoMantenimiento;

  titulo: string;
  descripcion: string;
  kilometrajeReporte: number | null;

  diagnostico: string | null;
  trabajoRealizado: string | null;
  tallerProveedor: string | null;
  costo: number | null;

  fechaReporte: string;
  fechaInicioAtencion: string | null;
  fechaReparacion: string | null;
  fechaCierre: string | null;
  fechaActualizacion: string | null;

  observacionesCierre: string | null;
}

export interface MantenimientoRequest {
  vehiculoId: number;
  tipoFalla: TipoFalla;
  prioridad: PrioridadMantenimiento;
  titulo: string;
  descripcion: string;
  kilometrajeReporte: number | null;
}

export interface EvaluarMantenimientoRequest {
  diagnostico: string;
  tallerProveedor: string | null;
  prioridad?: PrioridadMantenimiento;
}

export interface RepararMantenimientoRequest {
  trabajoRealizado: string;
  costo: number | null;
}

export interface CerrarMantenimientoRequest {
  observacionesCierre: string | null;
}

export interface EvidenciaMantenimiento {
  id: number;

  mantenimientoId: number;
  mantenimientoCodigo: string;

  vehiculoId: number;
  vehiculoPlaca: string;

  tipo: TipoEvidenciaMantenimiento;

  nombreOriginal: string;
  tipoContenido: string;
  tamanioBytes: number;

  descripcion: string | null;
  registradoPor: string;
  fechaRegistro: string;

  urlArchivo: string;
}

@Injectable({
  providedIn: 'root',
})
export class MantenimientoService {
  private readonly apiUrl = '/api/mantenimientos';

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Mantenimiento> {
    return this.http.get<Mantenimiento>(`${this.apiUrl}/${id}`);
  }

  listarPorVehiculo(vehiculoId: number): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(`${this.apiUrl}/vehiculo/${vehiculoId}`);
  }

  registrar(datos: MantenimientoRequest): Observable<Mantenimiento> {
    return this.http.post<Mantenimiento>(this.apiUrl, datos);
  }

  evaluar(id: number, datos: EvaluarMantenimientoRequest): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/evaluar`, datos);
  }

  iniciar(id: number): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/iniciar`, {});
  }

  reparar(id: number, datos: RepararMantenimientoRequest): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/reparar`, datos);
  }

  cerrar(id: number, datos: CerrarMantenimientoRequest): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/cerrar`, datos);
  }

  cancelar(id: number): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/cancelar`, {});
  }

  listarEvidencias(mantenimientoId: number): Observable<EvidenciaMantenimiento[]> {
    return this.http.get<EvidenciaMantenimiento[]>(`${this.apiUrl}/${mantenimientoId}/evidencias`);
  }

  registrarEvidencia(
    mantenimientoId: number,
    tipo: TipoEvidenciaMantenimiento,
    descripcion: string,
    archivo: File,
  ): Observable<EvidenciaMantenimiento> {
    const formulario = new FormData();

    formulario.append('tipo', tipo);

    formulario.append('descripcion', descripcion.trim());

    formulario.append('archivo', archivo, archivo.name);

    return this.http.post<EvidenciaMantenimiento>(
      `${this.apiUrl}/${mantenimientoId}/evidencias`,
      formulario,
    );
  }

  obtenerArchivoEvidencia(evidenciaId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/evidencias/${evidenciaId}/archivo`, {
      responseType: 'blob',
    });
  }
}
