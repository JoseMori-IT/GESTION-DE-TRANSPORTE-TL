import { describe, it, expect, vi } from 'vitest';
import {
  fechaFuturaValida,
  FechaHoraFuturaComponent,
  proximoMinutoLocal,
} from './fecha-hora-futura';
import { FormControl } from '@angular/forms';
describe('Fecha y hora futura', () => {
  const ahora = new Date(2026, 9, 23, 16, 30, 20);
  it('rechaza días anteriores, horas pasadas de hoy y el minuto actual', () => {
    for (const v of ['2026-10-22T20:00', '2026-10-23T15:00', '2026-10-23T16:30'])
      expect(fechaFuturaValida(v, ahora)).toBe(false);
  });
  it('admite una hora futura hoy y una hora temprana mañana', () => {
    expect(fechaFuturaValida('2026-10-23T16:31', ahora)).toBe(true);
    expect(fechaFuturaValida('2026-10-24T01:00', ahora)).toBe(true);
  });
  it('rechaza fechas inexistentes y campos incompletos', () => {
    for (const v of ['2026-02-30T10:00', '2026-10-23', '2026-10-23T', ''])
      expect(fechaFuturaValida(v, ahora)).toBe(false);
  });
  it('conserva el formato del formulario y valida aunque se escriba a mano', () => {
    const c = new FechaHoraFuturaComponent();
    try {
      c.writeValue('2026-10-23T17:00');
      expect(c.fecha).toBe('2026-10-23');
      expect(c.hora).toBe('17:00');
      expect(c.validate(new FormControl('2000-01-01T10:00'))).toEqual({ fechaPasada: true });
      expect(c.validate(new FormControl('2099-01-01T10:00'))).toBeNull();
    } finally {
      c.ngOnDestroy();
    }
  });
});

describe('Opciones de hora según el día', () => {
  it('solo ofrece minutos futuros hoy y todas las horas para mañana', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 31, 16, 30, 20));
    const c = new FechaHoraFuturaComponent();
    try {
      c.writeValue(proximoMinutoLocal());
      expect(c.fecha).toBe('2026-10-31');
      expect(c.hora).toBe('16:31');
      expect(c.horasDisponibles[0]).toBe('16:31');
      expect(c.horasDisponibles).not.toContain('16:30');
      c.writeValue('2026-11-01T08:00');
      expect(c.horasDisponibles[0]).toBe('00:00');
      c.writeValue('2026-10-30T08:00');
      expect(c.horasDisponibles).toEqual([]);
    } finally {
      c.ngOnDestroy();
      vi.useRealTimers();
    }
  });
  it('al cambiar el minuto elimina las horas vencidas y maneja medianoche', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 31, 23, 58, 20));
    const c = new FechaHoraFuturaComponent();
    try {
      c.writeValue(proximoMinutoLocal());
      expect(c.horasDisponibles).toEqual(['23:59']);
      vi.setSystemTime(new Date(2026, 9, 31, 23, 59, 20));
      c.actualizarAhora();
      expect(c.horasDisponibles).toEqual([]);
      expect(proximoMinutoLocal()).toBe('2026-11-01T00:00');
    } finally {
      c.ngOnDestroy();
      vi.useRealTimers();
    }
  });
});

describe('Límite de programación de la solicitud', () => {
  it('respeta fecha y hora requeridas y libera las horas del día posterior', () => {
    const c = new FechaHoraFuturaComponent();
    try {
      c.minima = '2099-10-23T14:30:00';
      c.writeValue('2099-10-23T14:30');
      expect(c.fechaMinima).toBe('2099-10-23');
      expect(c.horasDisponibles[0]).toBe('14:30');
      expect(c.validate(new FormControl('2099-10-23T14:29'))).toEqual({ fechaPasada: true });
      expect(c.validate(new FormControl('2099-10-23T14:30'))).toBeNull();
      c.writeValue('2099-10-24T00:00');
      expect(c.horasDisponibles[0]).toBe('00:00');
      expect(c.validate(new FormControl('2099-10-24T00:00'))).toBeNull();
    } finally {
      c.ngOnDestroy();
    }
  });
});
