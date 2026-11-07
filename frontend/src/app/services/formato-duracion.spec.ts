import { describe, it, expect } from 'vitest';
import { formatearDuracionMinutos as tiempo } from './formato-duracion';
describe('Duraciones legibles sin decimales', () => {
  it('convierte el atraso mostrado en horas y minutos enteros', () => {
    expect(tiempo(1341.7)).toBe('22 h 22 min');
  });
  it('redondea una vez antes de separar unidades', () => {
    expect(tiempo(59.6)).toBe('1 h');
    expect(tiempo(119.5)).toBe('2 h');
    expect(tiempo(1439.6)).toBe('1 d');
  });
  it('mantiene cero y minutos cortos como valores enteros', () => {
    expect(tiempo(0)).toBe('0 min');
    expect(tiempo(10.8)).toBe('11 min');
    expect(tiempo(7.1)).toBe('7 min');
    expect(tiempo(3.7)).toBe('4 min');
    expect(tiempo(0.4)).toBe('0 min');
  });
  it('distingue datos ausentes o invalidos de cero', () => {
    for (const valor of [null, undefined, NaN, Infinity, -1])
      expect(tiempo(valor)).toBe('Sin datos');
  });
  it('desglosa duraciones de varios dias sin minutos ambiguos', () => {
    expect(tiempo(1501)).toBe('1 d 1 h 1 min');
  });
});
