import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, EMPTY, throwError } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import { Vehiculos } from './vehiculos';
import { VehiculoService, Vehiculo } from '../../services/vehiculo.service';
import { SesionService } from '../../services/sesion.service';
import { DialogosService } from '../../services/dialogos.service';
function preparar() {
  sessionStorage.setItem('rol', 'COORDINADOR');
  const unidad = {
    id: 7,
    placa: 'ABC-123',
    marca: 'Prueba',
    modelo: 'Modelo',
    anioFabricacion: 2026,
    tipo: 'CAMION',
    capacidadPasajeros: 2,
    estado: 'DISPONIBLE',
  } as Vehiculo;
  const movimiento = {
    id: 1,
    vehiculoId: 7,
    accion: 'BAJA',
    estadoAnterior: 'DISPONIBLE',
    estadoNuevo: 'BAJA_DEFINITIVA',
    fechaEfectiva: '2026-10-29',
    motivo: 'Retiro definitivo',
    responsable: 'Coordinador',
    registrado: '2026-10-29T12:00:00',
  };
  const api = {
    historialSituacion: vi.fn(() => of([])),
    registrarSituacion: vi.fn(() => of(movimiento)),
    listar: vi.fn(() => of([unidad])),
  };
  const dialogos = { confirmar: vi.fn(() => Promise.resolve(true)) };
  TestBed.configureTestingModule({
    imports: [Vehiculos],
    providers: [
      { provide: VehiculoService, useValue: api },
      { provide: Router, useValue: { events: EMPTY } },
      { provide: DialogosService, useValue: dialogos },
      { provide: SesionService, useValue: {} },
    ],
  });
  const pagina = TestBed.createComponent(Vehiculos).componentInstance;
  pagina.vehiculos.set([unidad]);
  pagina.abrirGestionUnidad(unidad, 'BAJA');
  pagina.situacionForm.patchValue({ motivo: 'Retiro definitivo' });
  return { pagina, api, dialogos, unidad };
}
describe('Baja definitiva de unidades', () => {
  it('cancelar confirmación no registra baja y confirmar conserva historial', async () => {
    const { pagina, api, dialogos } = preparar();
    dialogos.confirmar.mockResolvedValueOnce(false);
    await pagina.registrarSituacionUnidad();
    expect(api.registrarSituacion).not.toHaveBeenCalled();
    expect(pagina.unidadGestion()?.estado).toBe('DISPONIBLE');
    await pagina.registrarSituacionUnidad();
    expect(dialogos.confirmar).toHaveBeenCalledWith(expect.stringContaining('ni reactivarse'));
    expect(api.registrarSituacion).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ accion: 'BAJA', confirmarBaja: true }),
    );
    expect(pagina.unidadGestion()?.estado).toBe('BAJA_DEFINITIVA');
    expect(pagina.movimientosUnidad()).toHaveLength(1);
    await pagina.registrarSituacionUnidad();
    expect(api.registrarSituacion).toHaveBeenCalledTimes(1);
  });
  it('bloqueo del servidor conserva unidad y muestra código pendiente', async () => {
    const { pagina, api } = preparar();
    api.registrarSituacion.mockReturnValue(
      throwError(() => ({ error: { mensaje: 'Resuelve el viaje SER-001' } })),
    );
    await pagina.registrarSituacionUnidad();
    expect(pagina.errorSituacion()).toContain('SER-001');
    expect(pagina.unidadGestion()?.estado).toBe('DISPONIBLE');
    expect(pagina.movimientosUnidad()).toHaveLength(0);
  });
  it('operador no puede registrar baja y filtros separan retiradas', async () => {
    const { pagina, api, unidad } = preparar();
    sessionStorage.setItem('rol', 'OPERADOR');
    await pagina.registrarSituacionUnidad();
    expect(api.registrarSituacion).not.toHaveBeenCalled();
    pagina.vehiculos.set([unidad, { ...unidad, id: 8, estado: 'BAJA_DEFINITIVA' }]);
    pagina.filtroSituacion.set('OPERATIVOS');
    expect(pagina.vehiculosFiltrados().map((v) => v.id)).toEqual([7]);
    pagina.filtroSituacion.set('BAJA_DEFINITIVA');
    expect(pagina.vehiculosFiltrados().map((v) => v.id)).toEqual([8]);
  });
});
