import { TestBed } from '@angular/core/testing';
import { describe, it, expect, vi } from 'vitest';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { Rutas } from './rutas';
function crear() {
  const api = {
    listar: vi.fn(() => of([])),
    listarUbicaciones: vi.fn(() => of([])),
    registrar: vi.fn(() =>
      throwError(() => ({ status: 409, error: { mensaje: 'Ruta duplicada' } })),
    ),
  };
  const c = TestBed.runInInjectionContext(
    () => new Rutas(api as never, new FormBuilder(), {} as never, {} as never, {} as never),
  );
  return { c, api };
}
function datos(c: Rutas) {
  c.rutaForm.patchValue({
    nombre: 'Prueba',
    origenUbicacionId: 1,
    destinoUbicacionId: 2,
    distanciaKm: 20,
  });
  c.mostrarFormulario.set(true);
}
describe('Errores recuperables del catálogo de rutas', () => {
  it('detecta el recorrido duplicado sin enviar ni bloquear el formulario', () => {
    const { c, api } = crear();
    datos(c);
    c.rutas.set([
      { id: 4, codigo: 'RUT-004', origenUbicacion: { id: 1 }, destinoUbicacion: { id: 2 } },
    ] as never);
    c.guardarRuta();
    expect(api.registrar).not.toHaveBeenCalled();
    expect(c.mensajeError()).toContain('RUT-004');
    expect(c.guardando()).toBe(false);
    expect(c.mostrarFormulario()).toBe(true);
    expect(c.rutas()).toHaveLength(1);
  });
  it('un conflicto del servidor se queda en el formulario sin convertirlo en error de carga', () => {
    const { c } = crear();
    datos(c);
    c.guardarRuta();
    expect(c.mensajeError()).toBe('Ruta duplicada');
    expect(c.errorCarga()).toBe('');
    expect(c.guardando()).toBe(false);
    expect(c.mostrarFormulario()).toBe(true);
    c.cerrarFormulario();
    expect(c.mensajeError()).toBe('');
  });
  it('volver a intentar limpia un error de carga al recibir el listado', () => {
    const { c, api } = crear();
    api.listar.mockReturnValueOnce(throwError(() => new Error('Red')));
    c.cargarDatos();
    expect(c.errorCarga()).not.toBe('');
    c.cargarRutas();
    expect(c.errorCarga()).toBe('');
    expect(c.cargando()).toBe(false);
    expect(api.listar).toHaveBeenCalledTimes(2);
  });
});
