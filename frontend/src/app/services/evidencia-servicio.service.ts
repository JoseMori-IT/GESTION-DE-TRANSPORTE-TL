import { abrirVistaPreviaEvidencia } from './vista-previa-evidencia';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoEvidencia =
  'FOTO_AVERIA' | 'FOTO_INICIO' | 'FOTO_CARGA' | 'FOTO_ENTREGA' | 'DOCUMENTO' | 'CONFORMIDAD';

export interface EvidenciaServicio {
  id: number;
  servicioId: number;
  servicioCodigo: string;
  tipo: TipoEvidencia;
  nombreOriginal: string;
  tipoContenido: string;
  tamanoBytes: number;
  descripcion: string | null;
  latitud: number | null;
  longitud: number | null;
  nombreResponsable: string | null;
  documentoResponsable: string | null;
  registradoPor: string;
  fechaRegistro: string;
  urlArchivo: string;
}

export interface EvidenciaServicioRequest {
  tipo: TipoEvidencia;
  archivo: File;
  descripcion?: string | null;
  latitud?: number | null;
  longitud?: number | null;
  nombreResponsable?: string | null;
  documentoResponsable?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class EvidenciaServicioService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/evidencias';

  listarPorServicio(servicioId: number): Observable<EvidenciaServicio[]> {
    return this.http.get<EvidenciaServicio[]>(`${this.apiUrl}/servicio/${servicioId}`);
  }

  registrar(servicioId: number, datos: EvidenciaServicioRequest): Observable<EvidenciaServicio> {
    const formulario = new FormData();

    formulario.append('tipo', datos.tipo);
    formulario.append('archivo', datos.archivo);

    this.agregarTexto(formulario, 'descripcion', datos.descripcion);

    this.agregarNumero(formulario, 'latitud', datos.latitud);

    this.agregarNumero(formulario, 'longitud', datos.longitud);

    this.agregarTexto(formulario, 'nombreResponsable', datos.nombreResponsable);

    this.agregarTexto(formulario, 'documentoResponsable', datos.documentoResponsable);

    /*
     * No establecer Content-Type manualmente.
     * El navegador genera automáticamente el boundary
     * necesario para multipart/form-data.
     */
    return this.http.post<EvidenciaServicio>(`${this.apiUrl}/servicio/${servicioId}`, formulario);
  }

  descargar(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/archivo`, {
      responseType: 'blob',
    });
  }

  abrirArchivo(evidencia: EvidenciaServicio): void {
    abrirVistaPreviaEvidencia(
      evidencia,
      () => this.descargar(evidencia.id),
      (mensaje) => window.alert(mensaje),
    );
  }

  private agregarTexto(
    formulario: FormData,
    nombre: string,
    valor: string | null | undefined,
  ): void {
    const texto = valor?.trim();

    if (texto) {
      formulario.append(nombre, texto);
    }
  }

  private agregarNumero(
    formulario: FormData,
    nombre: string,
    valor: number | null | undefined,
  ): void {
    if (valor !== null && valor !== undefined && Number.isFinite(valor)) {
      formulario.append(nombre, String(valor));
    }
  }
}
