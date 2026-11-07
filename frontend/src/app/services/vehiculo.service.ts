import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoVehiculo =
  'AUTOMOVIL' | 'CAMIONETA' | 'MINIBUS' | 'BUS' | 'CAMION' | 'MOTOCICLETA' | 'OTRO';

export type EstadoVehiculo =
  'DISPONIBLE' | 'EN_SERVICIO' | 'MANTENIMIENTO' | 'INACTIVO' | 'SUSPENDIDO' | 'BAJA_DEFINITIVA';

export interface Vehiculo {
  odometroKm?: number | null;
  estadoOdometro?: 'OPERATIVO' | 'AVERIADO' | null;
  fechaOdometro?: string | null;
  versionRegistro?: number;

  id: number;
  codigoInterno: string;
  placa: string;
  marca: string;
  modelo: string;
  anioFabricacion: number;
  tipo: TipoVehiculo;
  capacidadPasajeros: number;
  estado: EstadoVehiculo;
  fechaRegistro: string;
  fechaActualizacion: string | null;
  tieneFoto: boolean;
}

export interface VehiculoRequest {
  odometroKm?: number | null;
  estadoOdometro?: 'OPERATIVO' | 'AVERIADO' | null;
  fechaOdometro?: string | null;
  versionRegistro?: number;
  motivoOdometro?: string | null;
  actualizarOdometro?: boolean;

  codigoInterno?: string | null;
  placa: string;
  marca: string;
  modelo: string;
  anioFabricacion: number;
  tipo: TipoVehiculo;
  capacidadPasajeros: number;
  estado: EstadoVehiculo;
}

@Injectable({
  providedIn: 'root',
})
export class VehiculoService {
  private readonly apiUrl = '/api/vehiculos';

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Vehiculo[]> {
    return this.http.get<Vehiculo[]>(this.apiUrl);
  }

  registrar(datos: VehiculoRequest): Observable<Vehiculo> {
    return this.http.post<Vehiculo>(this.apiUrl, datos);
  }

  historialOdometro(id: number): Observable<MovimientoOdometro[]> {
    return this.http.get<MovimientoOdometro[]>(`${this.apiUrl}/${id}/odometro/historial`);
  }
  historialSituacion(id: number): Observable<MovimientoUnidad[]> {
    return this.http.get<MovimientoUnidad[]>(`${this.apiUrl}/${id}/situacion`);
  }
  registrarSituacion(id: number, datos: SituacionUnidadRequest): Observable<MovimientoUnidad> {
    return this.http.post<MovimientoUnidad>(`${this.apiUrl}/${id}/situacion`, datos);
  }

  gestionar(
    id: number,
    datos: VehiculoRequest,
    estadoObjetivo: EstadoVehiculo,
    mantenimiento: import('./mantenimiento.service').MantenimientoRequest | null,
  ): Observable<Vehiculo> {
    return this.http.post<Vehiculo>(`${this.apiUrl}/${id}/gestion`, {
      datos,
      estadoObjetivo,
      mantenimiento,
    });
  }

  actualizar(id: number, datos: VehiculoRequest): Observable<Vehiculo> {
    return this.http.put<Vehiculo>(`${this.apiUrl}/${id}`, datos);
  }

  cambiarEstado(id: number, estado: EstadoVehiculo): Observable<Vehiculo> {
    return this.http.patch<Vehiculo>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  desactivar(id: number): Observable<Vehiculo> {
    return this.http.patch<Vehiculo>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  guardarFoto(id: number, archivo: File): Observable<Vehiculo> {
    const formulario = new FormData();
    formulario.append('archivo', archivo);

    return this.http.post<Vehiculo>(`${this.apiUrl}/${id}/foto`, formulario);
  }

  obtenerFoto(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/foto`, { responseType: 'blob' });
  }

  eliminarFoto(id: number): Observable<Vehiculo> {
    return this.http.delete<Vehiculo>(`${this.apiUrl}/${id}/foto`);
  }
}

export interface SituacionUnidadRequest {
  accion: 'SUSPENDER' | 'REHABILITAR' | 'BAJA';
  fechaEfectiva: string;
  motivo: string;
  observaciones: string;
  referenciaSustento: string;
  confirmarBaja: boolean;
}
export interface MovimientoUnidad {
  id: number;
  vehiculoId: number;
  accion: string;
  estadoAnterior: string;
  estadoNuevo: string;
  fechaEfectiva: string;
  motivo: string;
  observaciones: string | null;
  referenciaSustento: string | null;
  usuario: string;
  responsable: string;
  registrado: string;
}

export interface MovimientoOdometro {
  id: number;
  tipo: string;
  servicioId: number | null;
  lecturaAnterior: number | null;
  lecturaNueva: number | null;
  estadoAnterior: string | null;
  estadoNuevo: string;
  motivo: string | null;
  usuario: string;
  fechaLectura: string;
  fechaRegistro: string;
}
