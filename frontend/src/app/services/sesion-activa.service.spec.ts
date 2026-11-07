import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { SesionActivaService } from './sesion-activa.service';
const token = () =>
  'prueba.' + btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 120 })) + '.firma';
describe('Renovación mientras se utiliza el sistema', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    Object.defineProperty(document, 'hidden', { configurable: true, value: false });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    sessionStorage.setItem('token', token());
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
    sessionStorage.clear();
    vi.useRealTimers();
  });
  it('renueva cerca del vencimiento y conserva el token confirmado', async () => {
    const s = TestBed.inject(SesionActivaService);
    const pendiente = s.comprobar();
    TestBed.inject(HttpTestingController).expectOne('/api/auth/renovar').flush({ token: 'nuevo' });
    await pendiente;
    expect(sessionStorage.getItem('token')).toBe('nuevo');
  });
  it('no mantiene viva la sesión solo por dejar una pestaña abierta', async () => {
    const s = TestBed.inject(SesionActivaService);
    vi.setSystemTime(Date.now() + 6 * 60000);
    sessionStorage.setItem('token', token());
    await s.comprobar();
    TestBed.inject(HttpTestingController).expectNone('/api/auth/renovar');
  });
  it('no restaura una sesión cerrada durante la petición', async () => {
    const s = TestBed.inject(SesionActivaService);
    const pendiente = s.comprobar();
    sessionStorage.removeItem('token');
    TestBed.inject(HttpTestingController).expectOne('/api/auth/renovar').flush({ token: 'nuevo' });
    await pendiente;
    expect(sessionStorage.getItem('token')).toBeNull();
  });
  it('no renueva el acceso local sin conexión ni una pestaña oculta', async () => {
    const s = TestBed.inject(SesionActivaService);
    sessionStorage.setItem('accesoOffline', 'true');
    await s.comprobar();
    sessionStorage.removeItem('accesoOffline');
    Object.defineProperty(document, 'hidden', { configurable: true, value: true });
    await s.comprobar();
    TestBed.inject(HttpTestingController).expectNone('/api/auth/renovar');
  });
});
