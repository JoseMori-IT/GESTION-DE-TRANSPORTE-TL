import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { Vehiculos } from './vehiculos';
import { VehiculoService, Vehiculo } from '../../services/vehiculo.service';
import { SesionService } from '../../services/sesion.service';
describe('Gestión directa de unidad', () => {
  it('envía datos, prioridad y mantenimiento juntos sin navegar a Flota', () => {
    const router = { navigate: vi.fn(), events: EMPTY };
    const vehiculo = {
      id: 7,
      placa: 'ABC-123',
      marca: 'Prueba',
      modelo: 'Modelo',
      anioFabricacion: 2026,
      tipo: 'CAMION',
      capacidadPasajeros: 2,
      estado: 'INACTIVO',
    } as Vehiculo;
    const api = {
      gestionar: vi.fn(() => of({ ...vehiculo, estado: 'MANTENIMIENTO' })),
      listar: vi.fn(() => of([])),
    };
    TestBed.configureTestingModule({
      imports: [Vehiculos],
      providers: [
        { provide: VehiculoService, useValue: api },
        { provide: Router, useValue: router },
        { provide: SesionService, useValue: {} },
      ],
    });
    const pagina = TestBed.createComponent(Vehiculos).componentInstance;
    pagina.editarVehiculo(vehiculo);
    expect(pagina.nombreEstado('INACTIVO')).toBe('Pendiente de clasificar');
    pagina.vehiculoForm.controls.estado.setValue('MANTENIMIENTO');
    expect(pagina.requiereMantenimiento()).toBe(true);
    pagina.guardarVehiculo();
    expect(api.gestionar).not.toHaveBeenCalled();
    pagina.mantenimientoForm.patchValue({
      titulo: 'Cambio de aceite',
      tipoFalla: 'MANTENIMIENTO_PREVENTIVO',
      prioridad: 'ALTA',
      descripcion: 'Atención planificada',
      kilometrajeReporte: 10000,
    });
    pagina.guardarVehiculo();
    expect(api.gestionar).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ estado: 'INACTIVO' }),
      'MANTENIMIENTO',
      expect.objectContaining({ vehiculoId: 7, prioridad: 'ALTA', titulo: 'Cambio de aceite' }),
    );
    expect(router.navigate).not.toHaveBeenCalled();
    expect(pagina.mostrarFormulario()).toBe(false);
  });
});
