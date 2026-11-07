import { Injectable } from '@angular/core';

export interface LugarEncontrado {
  nombre: string;
  latitud: number;
  longitud: number;
}
interface RespuestaPhoton {
  features?: Array<{
    geometry?: { type?: string; coordinates?: number[] };
    properties?: {
      name?: string;
      street?: string;
      city?: string;
      district?: string;
      county?: string;
      state?: string;
      country?: string;
    };
  }>;
}

@Injectable({ providedIn: 'root' })
export class BusquedaLugarService {
  private readonly cache = new Map<string, LugarEncontrado[]>();
  private readonly endpoint = 'https://photon.komoot.io/api/';

  async buscar(consulta: string, signal: AbortSignal): Promise<LugarEncontrado[]> {
    const clave = consulta.trim().toLocaleLowerCase();
    const guardados = this.cache.get(clave);
    if (guardados) return guardados;
    const params = new URLSearchParams({
      q: consulta.trim(),
      limit: '5',
      lat: '-8.38',
      lon: '-74.55',
    });
    const respuesta = await fetch(`${this.endpoint}?${params}`, { signal });
    if (!respuesta.ok) throw new Error('No se pudo consultar el buscador.');
    const datos: RespuestaPhoton = await respuesta.json();
    if (!Array.isArray(datos.features)) throw new Error('Respuesta inválida del buscador.');
    const lugares = datos.features.flatMap((f) => {
      const c = f.geometry?.coordinates;
      if (
        f.geometry?.type !== 'Point' ||
        !c ||
        !Number.isFinite(c[0]) ||
        !Number.isFinite(c[1]) ||
        Math.abs(c[1]) > 90 ||
        Math.abs(c[0]) > 180
      )
        return [];
      const p = f.properties ?? {};
      const nombre = [
        ...new Set(
          [p.name, p.street, p.district, p.city, p.county, p.state, p.country].filter(Boolean),
        ),
      ].join(', ');
      return nombre ? [{ nombre, latitud: c[1], longitud: c[0] }] : [];
    });
    if (this.cache.size >= 100) this.cache.clear();
    this.cache.set(clave, lugares);
    return lugares;
  }
}
