import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { EstadoServicio, ServicioTransporte } from './servicio-transporte.service';

@Injectable({
  providedIn: 'root',
})
export class ChoferService {
  private readonly http = inject(HttpClient);

  private readonly url = '/api/chofer/servicios';

  listarServicios(): Observable<ServicioTransporte[]> {
    return this.http.get<ServicioTransporte[]>(this.url);
  }

  cambiarEstado(servicioId: number, estado: EstadoServicio): Observable<ServicioTransporte> {
    return this.http.patch<ServicioTransporte>(`${this.url}/${servicioId}/estado`, { estado });
  }
}
