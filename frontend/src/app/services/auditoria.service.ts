import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type ResultadoAuditoria = 'EXITOSO' | 'FALLIDO' | 'RECHAZADO' | 'ERROR';

export interface Auditoria {
  id: number;

  usuarioId: number | null;
  usuarioNombre: string;
  usuarioCorreo: string | null;
  usuarioRol: string | null;

  accion: string;
  modulo: string;

  entidad: string | null;
  entidadId: string | null;
  codigoReferencia: string | null;

  detalle: string | null;
  valorAnterior: string | null;
  valorNuevo: string | null;

  resultado: ResultadoAuditoria;

  direccionIp: string | null;
  userAgent: string | null;
  fechaRegistro: string;
}

export interface PaginaAuditoria {
  content: Auditoria[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface FiltrosAuditoria {
  texto?: string;
  usuarioCorreo?: string;
  accion?: string;
  modulo?: string;
  resultado?: ResultadoAuditoria | '';
  fechaDesde?: string;
  fechaHasta?: string;
  pagina?: number;
  tamanio?: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuditoriaService {
  private readonly apiUrl = '/api/auditoria';

  constructor(private readonly http: HttpClient) {}

  consultar(filtros: FiltrosAuditoria): Observable<PaginaAuditoria> {
    let parametros = new HttpParams();

    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });

    return this.http.get<PaginaAuditoria>(this.apiUrl, {
      params: parametros,
    });
  }
}
