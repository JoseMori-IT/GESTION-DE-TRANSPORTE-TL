import { describe, expect, it } from 'vitest';
import { interpretarRecorrido } from './recorrido-vial.service';

describe('interpretarRecorrido', () => {
  it('convierte metros a km, segundos a minutos y GeoJSON a latitud/longitud', () => {
    expect(
      interpretarRecorrido({
        code: 'Ok',
        routes: [
          {
            distance: 45678,
            duration: 3661,
            geometry: {
              type: 'LineString',
              coordinates: [
                [-74.55, -8.38],
                [-74.8, -8.47],
              ],
            },
          },
        ],
      }),
    ).toEqual({
      distanciaKm: 45.68,
      duracionMinutos: 62,
      trayectoria: [
        [-8.38, -74.55],
        [-8.47, -74.8],
      ],
    });
  });
  it('no sustituye un camino inexistente por una distancia en línea recta', () => {
    expect(() => interpretarRecorrido({ code: 'NoRoute' })).toThrow(/manualmente/);
    expect(() => interpretarRecorrido({ code: 'NoSegment' })).toThrow(/manualmente/);
  });
  it('rechaza distancias y geometrías inválidas', () => {
    expect(() =>
      interpretarRecorrido({ code: 'Ok', routes: [{ distance: NaN, duration: 60 }] }),
    ).toThrow();
    expect(() =>
      interpretarRecorrido({
        code: 'Ok',
        routes: [
          {
            distance: 1000,
            duration: 60,
            geometry: {
              type: 'LineString',
              coordinates: [
                [-74, -8],
                [-74, 100],
              ],
            },
          },
        ],
      }),
    ).toThrow();
  });
});
