import { describe, it, expect } from 'vitest';
import { validarLecturaViaje } from './validacion-odometro';
import { evaluarDistancia } from './distancia-verificable';
import { ServicioTransporte } from './servicio-transporte.service';
import { tramosConPendientes } from '../components/avance-viaje';
import { MarcacionPendiente } from './marcacion-servicio.service';
const viaje = {
  id: 1,
  distanciaEstimadaKm: 30.74,
  tramos: [
    { orden: 1, odometroSalidaKm: 50000 },
    { orden: 2, odometroLlegadaKm: null },
  ],
} as ServicioTransporte;
describe('Odómetro del viaje y fuentes de distancia', () => {
  it('acepta salir con la lectura anterior, pero no una menor', () => {
    expect(validarLecturaViaje(viaje, true, 50000, false, '', 50000, false)).toBeNull();
    expect(validarLecturaViaje(viaje, true, 49999, false, '', 50000, false)).toContain('menor');
  });
  it('exige incremento al cerrar y detecta los 8000 km', () => {
    expect(validarLecturaViaje(viaje, false, 50000, false, '', 50000, false)).toContain('mayor');
    expect(validarLecturaViaje(viaje, false, 58000, false, '', 50000, false)).toContain('3 veces');
    expect(validarLecturaViaje(viaje, false, 50031, false, '', 50000, false)).toBeNull();
  });
  it('no permite ocultar errores con decimales o valores inválidos', () => {
    for (const n of [NaN, Infinity, -1, 100000000, 50000.001])
      expect(validarLecturaViaje(viaje, true, n, false, '', null, false)).not.toBeNull();
  });
  it('la excepción requiere un motivo y no inventa una lectura', () => {
    expect(validarLecturaViaje(viaje, true, null, true, '  ', 50000, true)).toContain('Describe');
    expect(
      validarLecturaViaje(viaje, true, null, true, 'Tablero averiado', 50000, true),
    ).toBeNull();
    expect(validarLecturaViaje(viaje, true, 50000, false, '', 50000, true)).toContain('averiado');
  });
  it('conserva excepción y motivo pendientes sin conexión', () => {
    const marca = {
      servicioId: 1,
      tramoOrden: 1,
      tipo: 'SALIDA_IDA',
      odometroKm: null,
      odometroNoDisponible: true,
      motivoOdometro: 'Tablero averiado',
      fechaHoraDispositivo: '2026-10-31T08:00:00',
    } as MarcacionPendiente;
    const tramos = tramosConPendientes(viaje, [marca]);
    expect(tramos[0].odometroNoDisponibleSalida).toBe(true);
    expect(tramos[0].motivoOdometroSalida).toBe('Tablero averiado');
    const d = evaluarDistancia({ ...viaje, tramos });
    expect(d.fuente).toBe('RUTA');
    expect(d.km).toBe(30.74);
    expect(d.motivo).toContain('averiado');
  });
  it('si tampoco hay ruta no muestra cero kilómetros', () => {
    const s = {
      ...viaje,
      distanciaEstimadaKm: null,
      tramos: [{ ...viaje.tramos![0], odometroNoDisponibleSalida: true }],
    } as ServicioTransporte;
    const d = evaluarDistancia(s);
    expect(d.fuente).toBe('SIN_DATOS');
    expect(d.km).toBeNull();
  });
});
