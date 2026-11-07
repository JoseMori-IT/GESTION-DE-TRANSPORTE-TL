import { describe, it, expect } from 'vitest';
import { fechaViaje, fechaHoraViaje, nombreProducto } from './presentacion-viaje';
import { ServicioTransporte } from './servicio-transporte.service';
describe('Presentación de viajes', () => {
  it('no inventa fechas cuando faltan marcaciones', () => {
    expect(fechaHoraViaje(null)).toBe('No registrada');
    expect(fechaViaje({} as ServicioTransporte)).toBe('Fecha no registrada');
  });
  it('prioriza la fecha real frente a la programada', () => {
    expect(
      fechaViaje({
        fechaHoraSalidaReal: '2026-11-07T10:00:00',
        fechaHoraSalida: '2026-11-08T10:00:00',
      } as ServicioTransporte),
    ).toBe('07/11/2026');
  });
  it('conserva productos nuevos y traduce los del catálogo', () => {
    expect(nombreProducto('RFF')).toBe('Racimo de Fruto Fresco');
    expect(nombreProducto('Producto nuevo')).toBe('Producto nuevo');
  });
});
