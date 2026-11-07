import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type EstadoRuta = 'ACTIVA' | 'INACTIVA';
export type TipoUbicacion = 'PLANTA' | 'ACOPIO' | 'PARCELA' | 'TALLER' | 'ALMACEN' | 'OTRO';

export interface Ubicacion {
  id: number;
  codigo: string;
  nombre: string;
  tipo: TipoUbicacion;
  latitud: number | null;
  longitud: number | null;
  referencia: string | null;
  estado: EstadoRuta;
}
export interface UbicacionRequest {
  nombre: string;
  tipo: TipoUbicacion;
  latitud: number;
  longitud: number;
  referencia: string;
  estado: EstadoRuta;
}
export interface Ruta {
  id: number;
  codigo: string;
  nombre: string;
  origen: string;
  destino: string;
  origenUbicacion: Ubicacion | null;
  destinoUbicacion: Ubicacion | null;
  distanciaKm: number;
  duracionEstimadaMinutos: number;
  observaciones: string | null;
  estado: EstadoRuta;
  fechaRegistro: string;
  fechaActualizacion: string | null;
}
export interface RutaRequest {
  codigo: string;
  nombre: string;
  origenUbicacionId: number;
  destinoUbicacionId: number;
  distanciaKm: number;
  duracionEstimadaMinutos: number;
  observaciones: string;
  estado: EstadoRuta;
}

@Injectable({ providedIn: 'root' })
export class RutaService {
  private readonly apiUrl = '/api/rutas';
  private readonly ubicacionesUrl = '/api/ubicaciones';
  constructor(private http: HttpClient) {}
  listar(): Observable<Ruta[]> {
    return this.http.get<Ruta[]>(this.apiUrl);
  }
  registrar(datos: RutaRequest): Observable<Ruta> {
    return this.http.post<Ruta>(this.apiUrl, datos);
  }
  actualizar(id: number, datos: RutaRequest): Observable<Ruta> {
    return this.http.put<Ruta>(`${this.apiUrl}/${id}`, datos);
  }
  desactivar(id: number): Observable<Ruta> {
    return this.http.patch<Ruta>(`${this.apiUrl}/${id}/desactivar`, {});
  }
  listarUbicaciones(): Observable<Ubicacion[]> {
    return this.http.get<Ubicacion[]>(this.ubicacionesUrl);
  }
  registrarUbicacion(datos: UbicacionRequest): Observable<Ubicacion> {
    return this.http.post<Ubicacion>(this.ubicacionesUrl, datos);
  }
  actualizarUbicacion(id: number, datos: UbicacionRequest): Observable<Ubicacion> {
    return this.http.put<Ubicacion>(`${this.ubicacionesUrl}/${id}`, datos);
  }
  desactivarUbicacion(id: number): Observable<Ubicacion> {
    return this.http.patch<Ubicacion>(`${this.ubicacionesUrl}/${id}/desactivar`, {});
  }
}
