import { describe, it, expect, vi, afterEach } from 'vitest';
import { of, Subject } from 'rxjs';
import { abrirVistaPreviaEvidencia } from './vista-previa-evidencia';
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});
describe('Vista previa de evidencia', () => {
  it('abre al pulsar, muestra carga y convierte la descarga genérica en imagen con nombre original', () => {
    vi.useFakeTimers();
    const doc = document.implementation.createHTMLDocument();
    const ventana = { document: doc, closed: false, opener: {}, close: vi.fn() };
    vi.spyOn(window, 'open').mockReturnValue(ventana as unknown as Window);
    const crear = vi.fn((_archivo: Blob) => 'blob:prueba');
    const revocar = vi.fn();
    vi.stubGlobal('URL', { createObjectURL: crear, revokeObjectURL: revocar });
    const respuesta = new Subject<Blob>();
    const error = vi.fn();
    abrirVistaPreviaEvidencia(
      { nombreOriginal: 'foto viaje.jpeg', tipoContenido: 'image/jpeg' },
      () => respuesta,
      error,
    );
    expect(doc.body.textContent).toContain('Cargando evidencia');
    respuesta.next(new Blob(['foto'], { type: 'application/octet-stream' }));
    expect(crear.mock.calls[0][0].type).toBe('image/jpeg');
    expect(doc.querySelector('img')?.getAttribute('src')).toBe('blob:prueba');
    expect(doc.querySelector('a')?.download).toBe('foto viaje.jpeg');
    expect(ventana.opener).toBeNull();
    expect(error).not.toHaveBeenCalled();
    ventana.closed = true;
    vi.advanceTimersByTime(2000);
    expect(revocar).toHaveBeenCalledWith('blob:prueba');
  });
  it('rechaza contenido activo sin abrir una ventana', () => {
    const abrir = vi.spyOn(window, 'open');
    const error = vi.fn();
    abrirVistaPreviaEvidencia(
      { nombreOriginal: 'archivo.html', tipoContenido: 'text/html' },
      () => of(new Blob()),
      error,
    );
    expect(abrir).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalled();
  });
});
