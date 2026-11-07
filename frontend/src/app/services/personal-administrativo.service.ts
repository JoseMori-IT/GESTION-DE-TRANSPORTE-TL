import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type CargoAdministrativo =
  'SUPERVISOR_TRANSPORTE' | 'OPERADOR_TRANSPORTE' | 'AUXILIAR_TRANSPORTE';

export type RolAdministrativo = 'COORDINADOR' | 'OPERADOR' | 'CONSULTA';

export interface PersonalAdministrativo {
  id: number;
  dni: string;
  nombres: string;
  apellidos: string;
  nombreCompleto: string;
  correo: string;

  cargo: CargoAdministrativo;
  rol: RolAdministrativo;
  activo: boolean;

  fechaIngreso: string;
  fechaCese: string | null;
  motivoDesactivacion: string | null;

  fechaCreacion: string;
  fechaActualizacion: string | null;

  fechaPrimerLogin: string | null;
  fechaUltimoLogin: string | null;
  fechaUltimaConexion: string | null;

  ultimaDireccionIp: string | null;
  ultimoUserAgent: string | null;

  procesosPermitidos: string[];
}

export interface RegistrarPersonalRequest {
  dni: string;
  nombres: string;
  apellidos: string;
  correo: string;
  contrasena: string;
  cargo: CargoAdministrativo;
  rol: RolAdministrativo;
  fechaIngreso: string;
}

export interface ActualizarPersonalRequest {
  dni: string;
  nombres: string;
  apellidos: string;
  correo: string;
  cargo: CargoAdministrativo;
  rol: RolAdministrativo;
  fechaIngreso: string;
}

export interface DesactivarPersonalRequest {
  fechaCese: string;
  motivo: string;
}

export interface ReactivarPersonalRequest {
  fechaReingreso: string;
}

@Injectable({
  providedIn: 'root',
})
export class PersonalAdministrativoService {
  private readonly apiUrl = '/api/personal-administrativo';

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<PersonalAdministrativo[]> {
    return this.http.get<PersonalAdministrativo[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<PersonalAdministrativo> {
    return this.http.get<PersonalAdministrativo>(`${this.apiUrl}/${id}`);
  }

  registrar(datos: RegistrarPersonalRequest): Observable<PersonalAdministrativo> {
    return this.http.post<PersonalAdministrativo>(this.apiUrl, datos);
  }

  actualizar(id: number, datos: ActualizarPersonalRequest): Observable<PersonalAdministrativo> {
    return this.http.put<PersonalAdministrativo>(`${this.apiUrl}/${id}`, datos);
  }

  desactivar(id: number, datos: DesactivarPersonalRequest): Observable<PersonalAdministrativo> {
    return this.http.patch<PersonalAdministrativo>(`${this.apiUrl}/${id}/desactivar`, datos);
  }

  reactivar(id: number, datos: ReactivarPersonalRequest): Observable<PersonalAdministrativo> {
    return this.http.patch<PersonalAdministrativo>(`${this.apiUrl}/${id}/reactivar`, datos);
  }
}
