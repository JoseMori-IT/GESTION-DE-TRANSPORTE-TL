import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MarcacionServicioService } from './marcacion-servicio.service';

describe('Aislamiento de marcaciones offline', () => {
  afterEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });
  it('consulta solo la cola del usuario actual, sin atribuir registros antiguos', async () => {
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: {} }] });
    const api = TestBed.inject(MarcacionServicioService);
    sessionStorage.setItem('usuarioId', '10');
    const datos = [
      { propietarioUsuarioId: '10', eventoUuid: 'propio', fechaHoraDispositivo: '2026-10-26' },
      { propietarioUsuarioId: '20', eventoUuid: 'ajeno', fechaHoraDispositivo: '2026-10-26' },
      { eventoUuid: 'antiguo', fechaHoraDispositivo: '2026-10-26' },
    ];
    const solicitud: any = { result: datos };
    vi.spyOn(api as any, 'abrirBaseDatos').mockResolvedValue({
      transaction: () => ({
        objectStore: () => ({
          getAll: () => {
            queueMicrotask(() => solicitud.onsuccess());
            return solicitud;
          },
        }),
      }),
    });
    expect((await api.pendientes()).map((m) => m.eventoUuid)).toEqual(['propio']);
  });
  it('no consulta una cola sin sesión identificada', async () => {
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: {} }] });
    const api = TestBed.inject(MarcacionServicioService);
    await expect(api.pendientes()).rejects.toThrow('Inicia sesión');
  });
});
