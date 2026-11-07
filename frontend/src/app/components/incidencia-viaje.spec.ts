import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { IncidenciaViajeComponent } from './incidencia-viaje';
import { ServicioTransporte, IncidenciaViaje } from '../services/servicio-transporte.service';
const viaje = (incidencias: IncidenciaViaje[] = []) =>
  ({ id: 1, estado: 'EN_CURSO', incidencias }) as ServicioTransporte;
describe('Acciones de avería simples y seguras', () => {
  beforeEach(() => {
    sessionStorage.setItem('rol', 'CHOFER');
    TestBed.configureTestingModule({
      imports: [IncidenciaViajeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });
  it('al chofer solo le muestra reportar y continuar cuando corresponde', () => {
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    f.componentRef.setInput(
      'servicio',
      viaje([{ id: 3, versionRegistro: 0, estado: 'REPORTADA' } as IncidenciaViaje]),
    );
    f.componentRef.setInput('chofer', true);
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain('Registrar técnico (opcional)');
    expect(f.nativeElement.textContent).not.toContain('Continuar mi viaje');
    expect(f.nativeElement.textContent).toContain('esperando atención');
    f.destroy();
  });
  it('envía el problema sin identidad elegida por el cliente y mantiene el formulario si falla', async () => {
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    f.componentRef.setInput('servicio', viaje());
    f.componentRef.setInput('chofer', true);
    f.detectChanges();
    const c = f.componentInstance;
    await c.elegir('REPORTAR');
    c.datos.descripcion = 'Se apagó';
    c.datos.lugar = 'Km 10';
    const envio = c.enviar('REPORTAR');
    const http = TestBed.inject(HttpTestingController);
    const r = http.expectOne('/api/incidencias-viaje/1');
    expect(r.request.body.usuario).toBeUndefined();
    expect(r.request.body.accion).toBe('REPORTAR');
    r.flush({ mensaje: 'Viaje cambió' }, { status: 409, statusText: 'Conflict' });
    await envio;
    expect(c.error()).toBe('Viaje cambió');
    expect(c.modo()).toBe('REPORTAR');
    expect(c.datos.descripcion).toBe('Se apagó');
    http.verify();
    f.destroy();
  });
  it('el coordinador puede elegir atención técnica o remolque', () => {
    sessionStorage.setItem('rol', 'COORDINADOR');
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    f.componentRef.setInput('servicio', viaje([{ id: 3, estado: 'REPORTADA' } as IncidenciaViaje]));
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Registrar técnico (opcional)');
    expect(f.nativeElement.textContent).toContain('La unidad llegó a planta');
    f.destroy();
  });
});

describe('Seguimiento de avería del coordinador', () => {
  it('distingue reporte pendiente de técnico asignado', async () => {
    const { AvanceViajeComponent } = await import('./avance-viaje');
    const c = new AvanceViajeComponent();
    c.servicio = viaje([{ id: 3, estado: 'REPORTADA' } as IncidenciaViaje]);
    expect(c.estadoActual).toBe('Avería reportada · esperando atención');
    c.servicio = viaje([{ id: 3, estado: 'TECNICO_EN_CAMINO' } as IncidenciaViaje]);
    expect(c.estadoActual).toContain('técnico asignado');
  });
});

describe('Aviso de ubicación GPS', () => {
  it('muestra espera, evita duplicados y permite reintentar tras un error', () => {
    TestBed.configureTestingModule({
      imports: [IncidenciaViajeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    let ok: PositionCallback = () => {};
    let fail: PositionErrorCallback = () => {};
    const obtener = vi.fn((a: PositionCallback, b: PositionErrorCallback) => {
      ok = a;
      fail = b;
    });
    vi.stubGlobal('navigator', {
      userAgent: navigator.userAgent,
      geolocation: { getCurrentPosition: obtener },
    });
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    try {
      f.componentRef.setInput('servicio', viaje());
      const c = f.componentInstance;
      c.modo.set('REPORTAR');
      c.ubicacion();
      f.detectChanges();
      expect(f.nativeElement.textContent).toContain('Obteniendo ubicación');
      c.ubicacion();
      expect(obtener).toHaveBeenCalledTimes(1);
      ok({ coords: { latitude: -8, longitude: -74 } } as GeolocationPosition);
      f.detectChanges();
      expect(c.obteniendoGps()).toBe(false);
      expect(f.nativeElement.textContent).toContain('Ubicación GPS agregada');
      c.ubicacion();
      fail({ code: 3 } as GeolocationPositionError);
      expect(c.obteniendoGps()).toBe(false);
      expect(c.error()).toContain('tardando demasiado');
    } finally {
      f.destroy();
      vi.unstubAllGlobals();
    }
  });
});

describe('Reanudación según el usuario', () => {
  for (const chofer of [true, false]) {
    it(`identifica la acción y pide confirmación (${chofer})`, () => {
      sessionStorage.setItem('rol', chofer ? 'CHOFER' : 'COORDINADOR');
      TestBed.configureTestingModule({
        imports: [IncidenciaViajeComponent],
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      const f = TestBed.createComponent(IncidenciaViajeComponent);
      f.componentRef.setInput(
        'servicio',
        viaje([{ id: 3, estado: 'LISTA_CONTINUAR' } as IncidenciaViaje]),
      );
      f.componentRef.setInput('chofer', chofer);
      f.detectChanges();
      const label = chofer ? 'Reanudar mi viaje' : 'Confirmar reanudación del viaje';
      const boton = Array.from(f.nativeElement.querySelectorAll('button')).find(
        (b: any) => b.textContent.trim() === label,
      ) as HTMLButtonElement;
      expect(boton).toBeTruthy();
      boton.click();
      f.detectChanges();
      expect(f.componentInstance.modo()).toBe('CONTINUAR');
      expect(f.nativeElement.textContent).toContain('la salida está coordinada');
      TestBed.inject(HttpTestingController).expectNone('/api/incidencias-viaje/1');
      f.destroy();
    });
  }
});

describe('Remolque sin doble confirmación', () => {
  it('explica el cierre automático y no pide odómetro de la unidad averiada', () => {
    sessionStorage.setItem('rol', 'COORDINADOR');
    TestBed.configureTestingModule({
      imports: [IncidenciaViajeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    f.componentRef.setInput(
      'servicio',
      viaje([{ id: 3, estado: 'REMOLQUE_ASIGNADO', servicioAuxilioId: 8 } as IncidenciaViaje]),
    );
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Se conserva su historial');
    expect(f.nativeElement.textContent).not.toContain('Confirmar regreso con auxilio');
    expect(f.nativeElement.querySelector('input[name=odometro]')).toBeNull();
    f.destroy();
  });
});

describe('Recepción en planta simple y autorizada', () => {
  it('requiere confirmación explícita y no pide recursos de rescate', async () => {
    sessionStorage.setItem('rol', 'COORDINADOR');
    TestBed.configureTestingModule({
      imports: [IncidenciaViajeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(IncidenciaViajeComponent);
    f.componentRef.setInput(
      'servicio',
      viaje([{ id: 3, versionRegistro: 0, estado: 'REPORTADA' } as IncidenciaViaje]),
    );
    f.detectChanges();
    const c = f.componentInstance;
    await c.elegir('RETORNO_PLANTA');
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Interrumpido por avería');
    expect(f.nativeElement.textContent).not.toContain('Asignar remolque');
    expect(f.nativeElement.querySelector('select')).toBeNull();
    await c.enviar('RETORNO_PLANTA');
    const http = TestBed.inject(HttpTestingController);
    http.expectNone('/api/incidencias-viaje/1');
    c.recepcionConfirmada = true;
    const guardar = c.enviar('RETORNO_PLANTA');
    const r = http.expectOne('/api/incidencias-viaje/1');
    expect(r.request.body.accion).toBe('RETORNO_PLANTA');
    r.flush({ id: 1, estado: 'INTERRUMPIDO', incidencias: [] });
    await guardar;
    expect(c.modo()).toBe('');
    http.verify();
    f.destroy();
  });
});
