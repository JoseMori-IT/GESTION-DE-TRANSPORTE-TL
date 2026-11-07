import { describe, it, expect } from 'vitest';
import { calcularIndicadores, minutos, promedio } from './indicadores-calculo';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
const ahora = Date.parse('2026-10-30T12:00:00');
const filtros = { desde: '', hasta: '', ruta: 'TODAS', chofer: 'TODOS' };
const viaje = (cambios: Partial<ServicioTransporte> = {}) =>
  ({
    id: 1,
    codigo: 'SER-1',
    vehiculo: { id: 1, placa: 'ABC' },
    conductor: { id: 1 },
    estado: 'COMPLETADO',
    origenDescripcion: 'Planta',
    destinoDescripcion: 'Planta',
    fechaHoraSalida: '2026-10-29T08:00:00',
    fechaHoraSalidaReal: '2026-10-29T08:10:00',
    fechaHoraLlegadaReal: '2026-10-29T10:10:00',
    distanciaEstimadaKm: 40,
    tramos: [
      {
        orden: 1,
        origen: 'Planta',
        destino: 'Acopio',
        salidaReal: '2026-10-29T08:10:00',
        llegadaReal: '2026-10-29T08:40:00',
        odometroSalidaKm: 100,
      },
      {
        orden: 2,
        origen: 'Acopio',
        destino: 'Planta',
        salidaReal: '2026-10-29T09:40:00',
        llegadaReal: '2026-10-29T10:10:00',
        odometroLlegadaKm: 150,
      },
    ],
    ...cambios,
  }) as ServicioTransporte;
describe('Indicadores con base verificable', () => {
  it('separa conducción, permanencia y duración total', () => {
    const d = calcularIndicadores([viaje()], filtros, ahora);
    expect(d.totalPromedio).toBe(120);
    expect(d.conduccionPromedio).toBe(60);
    expect(d.permanenciaPromedio).toBe(60);
    expect(d.desviacion).toBe(25);
  });
  it('excluye programados y cancelados del denominador de puntualidad', () => {
    const d = calcularIndicadores(
      [
        viaje(),
        viaje({ id: 2, estado: 'PROGRAMADO', fechaHoraSalidaReal: null }),
        viaje({ id: 3, estado: 'CANCELADO' }),
      ],
      filtros,
      ahora,
    );
    expect(d.iniciados.length).toBe(1);
    expect(d.puntuales.length).toBe(0);
    expect(d.atrasoPromedio).toBe(10);
    expect(d.cancelados).toBe(1);
    expect(d.pendientes.length).toBe(1);
  });
  it('no convierte datos incompletos en cero ni suma tramos parciales', () => {
    const s = viaje();
    s.tramos![1].llegadaReal = null;
    s.tramos![1].odometroLlegadaKm = 100;
    const d = calcularIndicadores([s], filtros, ahora);
    expect(d.conduccionPromedio).toBeNull();
    expect(d.sinOdometro.length).toBe(1);
    expect(d.desviacion).toBeNull();
    expect(promedio([])).toBeNull();
  });
  it('no acepta una secuencia de tramos solapada', () => {
    const s = viaje();
    s.tramos![1].salidaReal = '2026-10-29T08:20:00';
    const d = calcularIndicadores([s], filtros, ahora);
    expect(d.conduccionPromedio).toBeNull();
    expect(d.permanenciaPromedio).toBeNull();
  });
  it('filtra por fecha programada aunque la ejecución cambie de día', () => {
    const d = calcularIndicadores(
      [viaje({ fechaHoraSalidaReal: '2026-10-30T08:00:00' })],
      { ...filtros, desde: '2026-10-29', hasta: '2026-10-29' },
      ahora,
    );
    expect(d.servicios.length).toBe(1);
  });
  it('no calcula duraciones con fechas invertidas, futuras o inválidas', () => {
    expect(minutos('2026-10-30T10:00:00', '2026-10-30T09:00:00', ahora)).toBeNull();
    expect(minutos('2026-10-30T10:00:00', '2026-11-01T09:00:00', ahora)).toBeNull();
    expect(minutos('xxx', 'yyy', ahora)).toBeNull();
    expect(minutos('2026-10-30T10:00:00', '2026-10-30T10:00:00', ahora)).toBe(0);
  });
  it('compara kilómetros únicamente sobre los mismos viajes con ambos datos', () => {
    const s = viaje({ id: 2, distanciaEstimadaKm: null });
    const d = calcularIndicadores([viaje(), s], filtros, ahora);
    expect(d.pares).toBe(1);
    expect(d.kmReal).toBe(50);
    expect(d.kmPlan).toBe(40);
  });
  it('respeta ruta y chofer y excluye fechas ausentes al consultar un período', () => {
    const d = calcularIndicadores(
      [viaje({ fechaHoraSalida: null })],
      { ...filtros, desde: '2026-10-01' },
      ahora,
    );
    expect(d.servicios).toHaveLength(0);
    expect(d.omitidos).toBe(1);
    expect(
      calcularIndicadores([viaje()], { ...filtros, chofer: '2' }, ahora).servicios,
    ).toHaveLength(0);
  });
  it('filtra por estado con todas las rutas y combina estado, ruta y chofer', () => {
    const servicios = [
      viaje(),
      viaje({ id: 2, estado: 'CANCELADO' }),
      viaje({ id: 3, estado: 'PROGRAMADO', fechaHoraSalidaReal: null }),
    ];
    const d = calcularIndicadores(servicios, { ...filtros, estado: 'CANCELADO' }, ahora);
    expect(d.servicios.map((s) => s.id)).toEqual([2]);
    expect(d.completados).toHaveLength(0);
    expect(d.cancelados).toBe(1);
    expect(d.totalPromedio).toBeNull();
    expect(
      calcularIndicadores(
        servicios,
        { ...filtros, estado: 'COMPLETADO', ruta: 'Planta → Planta', chofer: '1' },
        ahora,
      ).servicios.map((s) => s.id),
    ).toEqual([1]);
    expect(
      calcularIndicadores(servicios, { ...filtros, estado: 'TODOS' }, ahora).servicios,
    ).toHaveLength(3);
  });
  it('separa la avería del tiempo de conducción y los viajes de auxilio de la productividad', () => {
    const s = viaje();
    s.incidencias = [
      { fechaReporte: '2026-10-29T08:15:00', fechaResolucion: '2026-10-29T08:35:00' },
    ] as ServicioTransporte['incidencias'];
    const d = calcularIndicadores(
      [s, viaje({ id: 2, servicioAuxiliadoId: 1 }), viaje({ id: 3, estado: 'INTERRUMPIDO' })],
      filtros,
      ahora,
    );
    expect(d.conduccionPromedio).toBe(40);
    expect(d.detalles[0].averia).toBe(20);
    expect(d.auxilios.length).toBe(1);
    expect(d.interrumpidos.length).toBe(1);
    expect(d.cancelados).toBe(0);
  });
});

describe('Atraso entre fechas diferentes', () => {
  it('conserva un atraso de 1341.7 minutos sin confundirlo con la duracion del viaje', () => {
    const s = viaje({
      fechaHoraSalida: '2026-10-27T17:50:00',
      fechaHoraSalidaReal: '2026-10-28T16:11:42',
      fechaHoraLlegadaReal: '2026-10-28T16:22:30',
    });
    const d = calcularIndicadores([s], filtros, ahora);
    expect(d.atrasoPromedio).toBeCloseTo(1341.7);
    expect(d.totalPromedio).toBeCloseTo(10.8);
  });
});
