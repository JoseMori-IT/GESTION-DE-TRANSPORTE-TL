import { describe, it, expect } from 'vitest';
import { Reportes } from './reportes';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
describe('Seguimiento impreso', () => {
  const reporte = Object.create(Reportes.prototype) as Reportes;
  const viaje = {
    tramos: [
      {
        orden: 2,
        origen: 'Destino',
        destino: 'Planta',
        salidaReal: '2026-10-24T09:30:00',
        llegadaReal: '2026-10-24T10:00:00',
        odometroLlegadaKm: 120,
      },
      {
        orden: 1,
        origen: 'Planta',
        destino: 'Destino',
        salidaReal: '2026-10-24T08:00:00',
        llegadaReal: '2026-10-24T08:30:00',
        odometroSalidaKm: 100,
      },
    ],
  } as ServicioTransporte;
  it('separa manejo y permanencia, ordenando los tramos', () => {
    expect(reporte.tiemposReporte(viaje, false)).toBe('1 h');
    expect(reporte.tiemposReporte(viaje, true)).toBe('1 h');
    expect(reporte.recorridoReporte(viaje)).toBe('Planta → Destino → Planta');
  });
  it('calcula kilómetros sin confundir datos faltantes con cero', () => {
    expect(reporte.controlOdometroReporte(viaje)).toContain('Real: 20 km');
    expect(reporte.controlOdometroReporte({ tramos: [] } as unknown as ServicioTransporte)).toBe(
      'Sin registro',
    );
  });
  it('señala lecturas inconsistentes', () => {
    const invalido = structuredClone(viaje);
    invalido.tramos![0].odometroLlegadaKm = 100;
    expect(reporte.controlOdometroReporte(invalido)).toContain('Revisar');
  });
  it('identifica tiempos parciales', () => {
    const parcial = structuredClone(viaje);
    parcial.tramos![0].llegadaReal = null;
    expect(reporte.tiemposReporte(parcial, false)).toBe('30 min (parcial)');
  });
});
