import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { Vehiculos } from './vehiculos';
import { VehiculoService, Vehiculo } from '../../services/vehiculo.service';
import { SesionService } from '../../services/sesion.service';
afterEach(() => sessionStorage.clear());
function pagina() {
  const api = {
    registrar: vi.fn(() => EMPTY),
    gestionar: vi.fn(() => EMPTY),
    listar: vi.fn(() => of([])),
  };
  TestBed.configureTestingModule({
    imports: [Vehiculos],
    providers: [
      { provide: VehiculoService, useValue: api },
      { provide: Router, useValue: { navigate: vi.fn(), events: EMPTY } },
      { provide: SesionService, useValue: {} },
    ],
  });
  const p = TestBed.createComponent(Vehiculos).componentInstance;
  p.abrirFormulario();
  p.vehiculoForm.patchValue({ placa: 'ABC-123', marca: 'Prueba', modelo: 'Camión' });
  return { p, api };
}
describe('Registro de unidad con odómetro', () => {
  it('exige lectura en altas y acepta cero como lectura real', () => {
    const { p, api } = pagina();
    p.guardarVehiculo();
    expect(api.registrar).not.toHaveBeenCalled();
    p.vehiculoForm.controls.odometroKm.setValue(0);
    p.guardarVehiculo();
    expect(api.registrar).toHaveBeenCalledWith(
      expect.objectContaining({ odometroKm: 0, estadoOdometro: 'OPERATIVO' }),
    );
  });
  it('acepta avería solo con motivo y envía lectura nula', () => {
    const { p, api } = pagina();
    p.vehiculoForm.patchValue({ estadoOdometro: 'AVERIADO', odometroKm: 100 });
    p.guardarVehiculo();
    expect(api.registrar).not.toHaveBeenCalled();
    p.vehiculoForm.controls.motivoOdometro.setValue('Tablero no funciona');
    p.guardarVehiculo();
    expect(api.registrar).toHaveBeenCalledWith(
      expect.objectContaining({
        odometroKm: null,
        estadoOdometro: 'AVERIADO',
        motivoOdometro: 'Tablero no funciona',
      }),
    );
  });
  it('editar datos generales no solicita ajuste automático del odómetro', () => {
    const { p, api } = pagina();
    const v = {
      id: 7,
      placa: 'ABC-123',
      marca: 'Prueba',
      modelo: 'Modelo',
      anioFabricacion: 2026,
      tipo: 'CAMION',
      capacidadPasajeros: 2,
      estado: 'DISPONIBLE',
      odometroKm: 50000,
      estadoOdometro: 'OPERATIVO',
      versionRegistro: 9,
    } as Vehiculo;
    p.editarVehiculo(v);
    p.guardarVehiculo();
    expect(api.gestionar).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ actualizarOdometro: false, versionRegistro: 9 }),
      'DISPONIBLE',
      null,
    );
  });
  it('ajuste requiere coordinador y motivo', () => {
    const { p, api } = pagina();
    p.editarVehiculo({
      id: 7,
      placa: 'ABC-123',
      marca: 'Prueba',
      modelo: 'Modelo',
      anioFabricacion: 2026,
      tipo: 'CAMION',
      capacidadPasajeros: 2,
      estado: 'DISPONIBLE',
      odometroKm: 50000,
      versionRegistro: 1,
    } as Vehiculo);
    p.vehiculoForm.patchValue({ actualizarOdometro: true, odometroKm: 100 });
    p.guardarVehiculo();
    expect(api.gestionar).not.toHaveBeenCalled();
    sessionStorage.setItem('rol', 'COORDINADOR');
    p.guardarVehiculo();
    expect(api.gestionar).not.toHaveBeenCalled();
    p.vehiculoForm.controls.motivoOdometro.setValue('Reemplazo del tablero');
    p.guardarVehiculo();
    expect(api.gestionar).toHaveBeenCalledWith(
      7,
      expect.objectContaining({
        actualizarOdometro: true,
        odometroKm: 100,
        motivoOdometro: 'Reemplazo del tablero',
      }),
      'DISPONIBLE',
      null,
    );
  });
});
