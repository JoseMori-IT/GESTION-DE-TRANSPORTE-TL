import { describe, it, expect } from 'vitest';
import { Abastecimiento, resumirAbastecimientos } from './abastecimientos';
describe('Abastecimientos reales separados de estimaciones', () => {
  it('filtra fechas y unidades y excluye anulados del total sin borrarlos del historial', () => {
    const filas = [
      { id: 1, vehiculoId: 1, fecha: '2026-10-01T08:00:00', galones: 25, anulado: false },
      { id: 2, vehiculoId: 1, fecha: '2026-10-02T08:00:00', galones: 10, anulado: true },
      { id: 3, vehiculoId: 2, fecha: '2026-11-01T08:00:00', galones: 15, anulado: false },
    ] as Abastecimiento[];
    const d = resumirAbastecimientos(filas, '2026-10-01', '2026-10-31', '1');
    expect(d.seleccion).toHaveLength(2);
    expect(d.galones).toBe(25);
    expect(d.vigentes).toHaveLength(1);
    expect(resumirAbastecimientos(filas, '', '', 'TODAS').galones).toBe(40);
  });
});
