import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type CategoriaLicencia = 'A_I' | 'A_IIA' | 'A_IIB' | 'A_IIIA' | 'A_IIIB' | 'A_IIIC';

export type EstadoConductor = 'DISPONIBLE' | 'EN_SERVICIO' | 'SUSPENDIDO' | 'INACTIVO';

export interface Conductor {
  id: number;
  dni: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string | null;
  numeroLicencia: string;
  categoriaLicencia: CategoriaLicencia;
  vencimientoLicencia: string;
  estado: EstadoConductor;
  fechaRegistro: string;
  fechaActualizacion: string | null;
  rolTripulacion: 'CHOFER' | 'COPILOTO' | 'AMBOS';
}

export interface ConductorRequest {
  dni: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  numeroLicencia: string;
  categoriaLicencia: CategoriaLicencia;
  vencimientoLicencia: string;
  estado: EstadoConductor;
}

export interface CuentaChofer {
  dni?: string | null;
  id: number;
  nombres: string;
  correo: string | null;
  rol: string;
  activo: boolean;
  conductorId: number | null;
  nombreConductor: string | null;
}

export interface AccesoTemporalChofer {
  usuarioId: number;
  identificador: string;
  contrasenaTemporal: string;
  venceEn: string;
}

@Injectable({
  providedIn: 'root',
})
export class ConductorService {
  private readonly apiUrl = '/api/conductores';

  constructor(private http: HttpClient) {}

  listar(): Observable<Conductor[]> {
    return this.http.get<Conductor[]>(this.apiUrl);
  }

  registrar(datos: ConductorRequest): Observable<Conductor> {
    return this.http.post<Conductor>(this.apiUrl, datos);
  }

  actualizar(id: number, datos: ConductorRequest): Observable<Conductor> {
    return this.http.put<Conductor>(`${this.apiUrl}/${id}`, datos);
  }

  desactivar(id: number): Observable<Conductor> {
    return this.http.patch<Conductor>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  listarCuentas(): Observable<CuentaChofer[]> {
    return this.http.get<CuentaChofer[]>('/api/usuarios');
  }

  crearAcceso(conductorId: number): Observable<AccesoTemporalChofer> {
    return this.http.post<AccesoTemporalChofer>(
      `/api/usuarios/chofer/${conductorId}/crear-acceso`,
      {},
    );
  }

  restablecerAcceso(usuarioId: number): Observable<AccesoTemporalChofer> {
    return this.http.post<AccesoTemporalChofer>(
      `/api/usuarios/${usuarioId}/restablecer-contrasena`,
      {
        motivo: 'Restablecimiento solicitado desde el módulo de conductores',
      },
    );
  }
}
