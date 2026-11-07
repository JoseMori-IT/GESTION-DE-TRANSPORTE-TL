import { DOCUMENT } from '@angular/common';
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('Autorización al desplegar bajo /transporte/', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
  });
  function enviar(url: string) {
    TestBed.configureTestingModule({
      providers: [
        { provide: DOCUMENT, useValue: { baseURI: 'https://10.181.8.8/transporte/' } },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    const next = vi.fn((_request: HttpRequest<unknown>) => of(new HttpResponse({ status: 200 })));
    let error: any;
    TestBed.runInInjectionContext(() =>
      authInterceptor(new HttpRequest('GET', url), next),
    ).subscribe({
      error: (e) => {
        error = e;
      },
    });
    return { next, error };
  }
  it('envía el token solo a la API local con su prefijo', () => {
    sessionStorage.setItem('token', 'prueba');
    const { next } = enviar('/api/chofer/servicios');
    const request = next.mock.calls[0][0] as HttpRequest<unknown>;
    expect(request.url).toBe('/transporte/api/chofer/servicios');
    expect(request.headers.get('Authorization')).toBe('Bearer prueba');
  });
  it('bloquea la API durante acceso local antes de enviar la petición', () => {
    sessionStorage.setItem('accesoOffline', 'true');
    const { next, error } = enviar('/api/evidencias/servicio/1');
    expect(next).not.toHaveBeenCalled();
    expect(error.status).toBe(0);
  });
  it('no envía el token a mapas externos', () => {
    sessionStorage.setItem('token', 'prueba');
    const { next } = enviar('https://photon.komoot.io/api/');
    const request = next.mock.calls[0][0] as HttpRequest<unknown>;
    expect(request.headers.has('Authorization')).toBe(false);
  });
});
