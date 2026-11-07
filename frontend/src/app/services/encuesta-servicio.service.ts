import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoEvaluador = 'INTERNO' | 'EXTERNO';

export interface EncuestaServicioRequest {
  puntualidad: number;
  cumplimiento: number;
  atencion: number;
  satisfaccionGeneral: number;
  comentarios: string;
}

export interface EncuestaServicioResponse {
  id: number;
  servicioId: number;
  servicioCodigo: string;
  puntualidad: number;
  cumplimiento: number;
  atencion: number;
  satisfaccionGeneral: number;
  promedio: number;
  comentarios: string | null;
  evaluadorNombre: string;
  evaluadorCorreo: string | null;
  tipoEvaluador: TipoEvaluador;
  fechaRegistro: string;
}

@Injectable({
  providedIn: 'root',
})
export class EncuestaServicioService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = '/api/encuestas';

  listar(): Observable<EncuestaServicioResponse[]> {
    return this.http.get<EncuestaServicioResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<EncuestaServicioResponse> {
    return this.http.get<EncuestaServicioResponse>(`${this.apiUrl}/${id}`);
  }

  buscarPorServicio(servicioId: number): Observable<EncuestaServicioResponse> {
    return this.http.get<EncuestaServicioResponse>(`${this.apiUrl}/servicio/${servicioId}`);
  }

  registrar(
    servicioId: number,
    request: EncuestaServicioRequest,
  ): Observable<EncuestaServicioResponse> {
    return this.http.post<EncuestaServicioResponse>(
      `${this.apiUrl}/servicio/${servicioId}`,
      request,
    );
  }
}
