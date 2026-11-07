import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MarcacionServicioService, MarcacionPendiente } from './marcacion-servicio.service';
import { FotosOfflineService } from './fotos-offline.service';

describe('Evidencias antes del cierre offline', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
  });
  function preparar(falla: boolean) {
    sessionStorage.setItem('usuarioId', '10');
    const orden: string[] = [];
    const post = vi.fn(() => {
      orden.push('llegada');
      return of({});
    });
    const sincronizar = vi.fn(async () => {
      orden.push('foto');
      if (falla) throw new Error('Servidor no disponible');
    });
    TestBed.configureTestingModule({
      providers: [
        { provide: HttpClient, useValue: { post } },
        { provide: FotosOfflineService, useValue: { sincronizar } },
      ],
    });
    const api = TestBed.inject(MarcacionServicioService);
    const fila = {
      propietarioUsuarioId: '10',
      servicioId: 1,
      eventoUuid: 'evento',
      tipo: 'LLEGADA_DESTINO',
      intentosSincronizacion: 0,
    } as MarcacionPendiente;
    vi.spyOn(api, 'pendientes').mockResolvedValueOnce([fila]).mockResolvedValue([]);
    vi.spyOn(api as any, 'eliminar').mockResolvedValue(undefined);
    const guardar = vi.spyOn(api as any, 'guardar').mockResolvedValue(undefined);
    return { api, orden, post, guardar };
  }
  it('envía fotografías antes de la marcación de llegada', async () => {
    const { api, orden } = preparar(false);
    await api.sincronizarPendientes([1]);
    expect(orden).toEqual(['foto', 'llegada']);
  });
  it('detiene el cierre y conserva la marcación si la fotografía no se confirma', async () => {
    const { api, post, guardar } = preparar(true);
    await api.sincronizarPendientes([1]);
    expect(post).not.toHaveBeenCalled();
    expect(guardar).toHaveBeenCalled();
  });
});
