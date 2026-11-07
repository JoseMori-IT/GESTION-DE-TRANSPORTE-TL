import { Injectable } from '@angular/core';
import { ServicioTransporte } from './servicio-transporte.service';

@Injectable({ providedIn: 'root' })
export class ViajesDispositivoService {
  private usuario(): string {
    const id = sessionStorage.getItem('usuarioId');
    if (!id || sessionStorage.getItem('rol') !== 'CHOFER')
      throw new Error('Inicia sesión como chofer para preparar el viaje.');
    return id;
  }
  guardar(servicios: ServicioTransporte[]): string {
    const propietario = this.usuario();
    const fecha = new Date().toISOString();
    // La consulta offline no necesita documentos, licencias ni datos del copiloto.
    const activos = servicios
      .filter((s) => ['PROGRAMADO', 'EN_CURSO'].includes(s.estado))
      .map((s) => ({
        ...s,
        conductor: {
          ...s.conductor,
          numeroDocumento: null,
          numeroLicencia: null,
          categoriaLicencia: null,
          vencimientoLicencia: null,
        },
        copiloto: null,
      }));
    localStorage.setItem('viajes-chofer-' + propietario, JSON.stringify(activos));
    localStorage.setItem(
      'viajes-chofer-meta-' + propietario,
      JSON.stringify({ propietario, fecha }),
    );
    return fecha;
  }
  leer(): { servicios: ServicioTransporte[]; fecha: string } | null {
    const propietario = this.usuario();
    const meta = JSON.parse(localStorage.getItem('viajes-chofer-meta-' + propietario) || 'null');
    const servicios = JSON.parse(localStorage.getItem('viajes-chofer-' + propietario) || 'null');
    if (
      meta?.propietario !== propietario ||
      !Number.isFinite(Date.parse(meta.fecha)) ||
      !Array.isArray(servicios)
    )
      return null;
    return { servicios, fecha: meta.fecha };
  }
}
