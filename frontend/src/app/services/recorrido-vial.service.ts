import { Injectable } from '@angular/core';

export interface CoordenadaRuta {
  latitud: number;
  longitud: number;
}
export interface RecorridoVial {
  distanciaKm: number;
  duracionMinutos: number;
  trayectoria: [number, number][];
}
interface RespuestaOSRM {
  code?: string;
  routes?: Array<{
    distance?: number;
    duration?: number;
    geometry?: { type?: string; coordinates?: number[][] };
  }>;
}

export function interpretarRecorrido(datos: RespuestaOSRM): RecorridoVial {
  if (datos.code === 'NoRoute' || datos.code === 'NoSegment') {
    throw new Error(
      'No se encontró un camino conectado cerca de estos puntos. Ingresa los kilómetros y la duración manualmente.',
    );
  }
  const ruta = datos.routes?.[0];
  const coordenadas = ruta?.geometry?.coordinates;
  if (
    datos.code !== 'Ok' ||
    !ruta ||
    !Number.isFinite(ruta.distance) ||
    ruta.distance! <= 0 ||
    !Number.isFinite(ruta.duration) ||
    ruta.duration! <= 0 ||
    ruta.geometry?.type !== 'LineString' ||
    !coordenadas ||
    coordenadas.length < 2 ||
    coordenadas.some(
      (c) =>
        c.length < 2 ||
        !Number.isFinite(c[0]) ||
        !Number.isFinite(c[1]) ||
        Math.abs(c[0]) > 180 ||
        Math.abs(c[1]) > 90,
    )
  ) {
    throw new Error(
      'El servicio no devolvió un recorrido válido. Puedes ingresar los kilómetros y la duración manualmente.',
    );
  }
  return {
    distanciaKm: Math.max(0.01, Number((ruta.distance! / 1000).toFixed(2))),
    duracionMinutos: Math.max(1, Math.ceil(ruta.duration! / 60)),
    trayectoria: coordenadas.map((c) => [c[1], c[0]]),
  };
}

@Injectable({ providedIn: 'root' })
export class RecorridoVialService {
  // Servicio público: consultas explícitas, caché y como máximo una solicitud por segundo.
  private readonly endpoint = 'https://routing.openstreetmap.de/routed-car/route/v1/driving/';
  private readonly cache = new Map<string, RecorridoVial>();
  private disponibleDesde = 0;

  async calcular(
    origen: CoordenadaRuta,
    destino: CoordenadaRuta,
    signal: AbortSignal,
  ): Promise<RecorridoVial> {
    if (
      [origen, destino].some(
        (p) =>
          !Number.isFinite(p.latitud) ||
          !Number.isFinite(p.longitud) ||
          Math.abs(p.latitud) > 90 ||
          Math.abs(p.longitud) > 180,
      )
    )
      throw new Error('Revisa las coordenadas de origen y destino.');
    const clave = `${origen.longitud},${origen.latitud};${destino.longitud},${destino.latitud}`;
    const guardado = this.cache.get(clave);
    if (guardado) return guardado;
    const ahora = Date.now();
    const turno = Math.max(ahora, this.disponibleDesde);
    this.disponibleDesde = turno + 1100;
    if (turno > ahora) await new Promise((resolve) => setTimeout(resolve, turno - ahora));
    signal.throwIfAborted();
    const respuesta = await fetch(
      `${this.endpoint}${clave}?overview=full&geometries=geojson&steps=false&radiuses=500;500`,
      { signal },
    );
    const datos: RespuestaOSRM = await respuesta.json();
    if (!respuesta.ok && datos.code !== 'NoRoute' && datos.code !== 'NoSegment')
      throw new Error(
        'No se pudo consultar el servicio de carreteras. Vuelve a intentar o ingresa la distancia manualmente.',
      );
    const recorrido = interpretarRecorrido(datos);
    if (this.cache.size >= 100) this.cache.clear();
    this.cache.set(clave, recorrido);
    return recorrido;
  }
}
