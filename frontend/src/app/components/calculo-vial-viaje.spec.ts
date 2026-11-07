import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CalculoVialViajeComponent } from './calculo-vial-viaje';
import { RecorridoVialService } from '../services/recorrido-vial.service';

const puntos = [
  { id: 1, nombre: 'Tulumayo', latitud: -8, longitud: -74 },
  { id: 2, nombre: 'Destino', latitud: -8.5, longitud: -74.5 },
  { id: 1, nombre: 'Tulumayo', latitud: -8, longitud: -74 },
];
describe('Cálculo vial completo del viaje', () => {
  let calcular: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    calcular = vi.fn();
    TestBed.configureTestingModule({
      imports: [CalculoVialViajeComponent],
      providers: [{ provide: RecorridoVialService, useValue: { calcular } }],
    });
  });
  afterEach(() => TestBed.resetTestingModule());
  it('consulta ambos sentidos y suma las distancias sin promediarlas', async () => {
    calcular
      .mockResolvedValueOnce({
        distanciaKm: 40,
        duracionMinutos: 50,
        trayectoria: [
          [-8, -74],
          [-8.5, -74.5],
        ],
      })
      .mockResolvedValueOnce({
        distanciaKm: 45,
        duracionMinutos: 60,
        trayectoria: [
          [-8.5, -74.5],
          [-8, -74],
        ],
      });
    const fixture = TestBed.createComponent(CalculoVialViajeComponent);
    const c = fixture.componentInstance;
    c.puntos = puntos;
    c.ngOnChanges();
    await c.calcular();
    expect(calcular).toHaveBeenCalledTimes(2);
    expect(c.totalKm).toBe(85);
    expect(c.totalMinutos).toBe(110);
    expect(c.completo).toBe(true);
    expect(c.trazados).toHaveLength(2);
    c.manual(0, 'duracionMinutos', 70);
    expect(c.trazados).toHaveLength(2);
    c.manual(0, 'distanciaKm', 42);
    expect(c.trazados).toHaveLength(1);
    expect(c.totalKm).toBe(87);
  });
  it('no inventa una línea recta cuando no existe carretera', async () => {
    calcular.mockRejectedValue(new Error('NoRoute'));
    const fixture = TestBed.createComponent(CalculoVialViajeComponent);
    const c = fixture.componentInstance;
    c.puntos = puntos;
    c.ngOnChanges();
    await c.calcular();
    expect(c.trazados).toHaveLength(0);
    expect(c.completo).toBe(false);
    c.manual(0, 'distanciaKm', 30);
    c.manual(0, 'duracionMinutos', 40);
    c.manual(1, 'distanciaKm', 35);
    c.manual(1, 'duracionMinutos', 50);
    expect(c.completo).toBe(true);
    expect(c.totalKm).toBe(65);
    expect(c.trazados).toHaveLength(0);
  });
  it('descarta el resultado anterior si se cambian las paradas durante la consulta', async () => {
    let resolver!: (valor: unknown) => void;
    calcular.mockImplementation(() => new Promise((r) => (resolver = r)));
    const fixture = TestBed.createComponent(CalculoVialViajeComponent);
    const c = fixture.componentInstance;
    c.puntos = puntos;
    c.ngOnChanges();
    const pendiente = c.calcular();
    c.puntos = [
      puntos[0],
      { id: 3, nombre: 'Otro destino', latitud: -9, longitud: -75 },
      puntos[0],
    ];
    c.ngOnChanges();
    resolver({
      distanciaKm: 40,
      duracionMinutos: 50,
      trayectoria: [
        [-8, -74],
        [-8.5, -74.5],
      ],
    });
    await pendiente;
    expect(c.trazados).toHaveLength(0);
    expect(c.completo).toBe(false);
    expect(c.segmentos[0].nombre).toContain('Otro destino');
  });
});
