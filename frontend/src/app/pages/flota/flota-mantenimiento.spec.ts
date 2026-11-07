import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, EMPTY } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { Flota } from './flota';
import { VehiculoService, Vehiculo } from '../../services/vehiculo.service';
import { ServicioTransporteService } from '../../services/servicio-transporte.service';
import { MantenimientoService } from '../../services/mantenimiento.service';
import { SesionService } from '../../services/sesion.service';
describe('Ingreso de una unidad disponible a mantenimiento', () => {
  it('abre confirmación y crea atención antes de mostrar mantenimiento', () => {
    const vehiculo = {
      id: 7,
      placa: 'ABC-123',
      marca: 'Prueba',
      modelo: 'Modelo',
      anioFabricacion: 2026,
      tipo: 'CAMION',
      capacidadPasajeros: 2,
      estado: 'DISPONIBLE',
      tieneFoto: false,
    } as Vehiculo;
    const unidades = { listar: vi.fn(() => of([vehiculo])), cambiarEstado: vi.fn() };
    const mantenimiento = { registrar: vi.fn(() => of({ id: 9 })) };
    TestBed.configureTestingModule({
      imports: [Flota],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: VehiculoService, useValue: unidades },
        { provide: ServicioTransporteService, useValue: { listar: () => of([]) } },
        { provide: MantenimientoService, useValue: mantenimiento },
        { provide: Router, useValue: { events: EMPTY } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ unidad: '7' }) } },
        },
        { provide: SesionService, useValue: {} },
      ],
    });
    sessionStorage.setItem('rol', 'COORDINADOR');
    const fixture = TestBed.createComponent(Flota);
    fixture.detectChanges();
    const boton = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((b) => b.textContent?.trim() === 'Enviar a mantenimiento');
    expect(boton).toBeTruthy();
    boton!.click();
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    expect(pagina.mantenimientoPendiente()?.id).toBe(7);
    expect(mantenimiento.registrar).not.toHaveBeenCalled();
    pagina.tituloMantenimiento.set('Cambio de aceite');
    pagina.prioridadMantenimiento.set('ALTA');
    pagina.confirmarEnvioMantenimiento();
    expect(mantenimiento.registrar).toHaveBeenCalledWith(
      expect.objectContaining({ vehiculoId: 7, prioridad: 'ALTA', titulo: 'Cambio de aceite' }),
    );
    expect(unidades.cambiarEstado).not.toHaveBeenCalled();
    expect(pagina.vehiculos()[0].estado).toBe('MANTENIMIENTO');
    expect(pagina.mantenimientoPendiente()).toBeNull();
  });
});
