import { TestBed } from '@angular/core/testing';
import { describe, it, expect, afterEach } from 'vitest';
import { DialogosService } from './dialogos.service';
describe('Confirmaciones de la aplicación', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('solo confirma cuando el usuario pulsa Confirmar', async () => {
    const s = TestBed.inject(DialogosService),
      p = s.confirmar('Desactivar ruta');
    expect(s.actual()?.mensaje).toBe('Desactivar ruta');
    s.resolver(true);
    expect(await p).toBe(true);
    expect(s.actual()).toBeNull();
  });
  it('cancelar no autoriza la operación y permite volver a abrir', async () => {
    const s = TestBed.inject(DialogosService),
      p = s.confirmar('Desactivar');
    s.resolver(false);
    expect(await p).toBe(false);
    const otra = s.confirmar('Reactivar');
    s.resolver(true);
    expect(await otra).toBe(true);
  });
  it('evita encolar la misma acción por doble clic', async () => {
    const s = TestBed.inject(DialogosService),
      primera = s.confirmar('Acción');
    expect(await s.confirmar('Otra')).toBe(false);
    s.resolver(false);
    expect(await primera).toBe(false);
  });
  it('muestra avisos informativos con la misma ventana', async () => {
    const s = TestBed.inject(DialogosService),
      p = s.informar('Selecciona un chofer');
    expect(s.actual()?.informativo).toBe(true);
    s.resolver(true);
    await p;
    expect(s.actual()).toBeNull();
  });
});
