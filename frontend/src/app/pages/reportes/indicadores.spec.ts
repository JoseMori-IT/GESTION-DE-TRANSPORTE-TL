import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Indicadores } from './indicadores';
describe('Fuentes de indicadores', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Indicadores],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  it('distingue una fuente no disponible de una consulta vacía', () => {
    const fixture = TestBed.createComponent(Indicadores);
    fixture.componentRef.setInput('servicios', []);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/mantenimientos')
      .flush('No disponible', { status: 503, statusText: 'Unavailable' });
    http.expectOne('/api/encuestas').flush([]);
    http.expectOne('/api/validaciones-viaje').flush([]);
    fixture.detectChanges();
    expect(fixture.componentInstance.mantenimiento()).toBeNull();
    expect(fixture.componentInstance.calidad().evaluados).toBe(0);
    expect(fixture.componentInstance.errores()).toEqual(['mantenimientos']);
    expect(fixture.componentInstance.pendientes()).toBe(0);
    http.verify();
    fixture.destroy();
  });
  it('mantiene los abiertos anteriores al período y excluye costos desconocidos', () => {
    const fixture = TestBed.createComponent(Indicadores);
    fixture.componentRef.setInput('servicios', []);
    fixture.componentRef.setInput('desde', '2026-10-01');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/mantenimientos').flush([
      { id: 1, fechaReporte: '2026-09-01', estado: 'REPORTADO', prioridad: 'CRITICA' },
      { id: 2, fechaReporte: '2026-10-01', estado: 'CERRADO', fechaCierre: '2026-10-02', costo: 0 },
      {
        id: 3,
        fechaReporte: '2026-10-01',
        estado: 'CERRADO',
        fechaCierre: '2026-10-02',
        costo: null,
      },
    ]);
    http.expectOne('/api/encuestas').flush([]);
    http.expectOne('/api/validaciones-viaje').flush([]);
    const m = fixture.componentInstance.mantenimiento()!;
    expect(m.abiertos.length).toBe(1);
    expect(m.criticos.length).toBe(1);
    expect(m.periodo.length).toBe(2);
    expect(m.costo).toBe(0);
    expect(m.sinCosto).toBe(1);
    http.verify();
    fixture.destroy();
  });
  it('el gráfico de tiempos compara la misma muestra de viajes completos', () => {
    const fixture = TestBed.createComponent(Indicadores);
    fixture.componentRef.setInput('servicios', []);
    const componente = fixture.componentInstance;
    const detalle = (conduccion: number | null, permanencia: number | null) => ({
      conduccion,
      permanencia,
    });
    const datos = { detalles: [detalle(30, 60), detalle(90, 20), detalle(300, null)] };
    Object.defineProperty(componente, 'datos', { value: () => datos });
    expect(componente.tiemposGrafico().muestra).toBe(2);
    expect(componente.tiemposGrafico().barras.map((b) => b.valor)).toEqual([60, 40]);
    fixture.destroy();
  });
  it('el gráfico de unidades permite cambiar la medida sin tratar km ausentes como cero', () => {
    const fixture = TestBed.createComponent(Indicadores);
    fixture.componentRef.setInput('servicios', []);
    const c = fixture.componentInstance;
    Object.defineProperty(c, 'unidades', {
      value: () => [
        { placa: 'A', completados: 3, km: 0, muestra: 0 },
        { placa: 'B', completados: 1, km: 100, muestra: 1 },
      ],
    });
    expect(c.unidadesGrafico().map((b) => b.nombre)).toEqual(['A', 'B']);
    c.medidaUnidad.set('km');
    expect(c.unidadesGrafico().map((b) => b.nombre)).toEqual(['B']);
    expect(c.anchoBarra(0, [{ valor: 0 }])).toBe(0);
    fixture.destroy();
  });
  it('el reporte individual no consulta ni presenta mantenimiento de la flota', () => {
    const fixture = TestBed.createComponent(Indicadores);
    fixture.componentRef.setInput('servicios', []);
    fixture.componentRef.setInput('vista', 'chofer');
    fixture.componentRef.setInput('chofer', '1');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectNone('/api/mantenimientos');
    http.expectOne('/api/encuestas').flush([]);
    http.expectOne('/api/validaciones-viaje').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Avance de mantenimiento');
    expect(fixture.nativeElement.textContent).not.toContain('Tolerancia');
    expect(fixture.nativeElement.textContent).toContain('Tiempo de atraso por viaje');
    http.verify();
    fixture.destroy();
  });
});
