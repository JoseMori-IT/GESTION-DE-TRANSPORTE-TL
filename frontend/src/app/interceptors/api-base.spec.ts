import { describe, expect, it } from 'vitest';
import { apiDesdeBase } from './api-base';

describe('API bajo el directorio de la aplicación', () => {
  it('conserva parámetros y caracteres escapados bajo /transporte/', () => {
    expect(
      apiDesdeBase('/api/rutas?nombre=Campo%20Verde&activo=true', 'https://10.181.8.8/transporte/'),
    ).toBe('/transporte/api/rutas?nombre=Campo%20Verde&activo=true');
  });
  it('mantiene el proxy /api/ en desarrollo', () => {
    expect(apiDesdeBase('/api/auth/login', 'http://localhost:4200/')).toBe('/api/auth/login');
  });
  it('no modifica proveedores externos ni duplica el prefijo', () => {
    expect(apiDesdeBase('https://photon.komoot.io/api/', 'https://10.181.8.8/transporte/')).toBe(
      'https://photon.komoot.io/api/',
    );
    expect(apiDesdeBase('/transporte/api/estado', 'https://10.181.8.8/transporte/')).toBe(
      '/transporte/api/estado',
    );
  });
});
