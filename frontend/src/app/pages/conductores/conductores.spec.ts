import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { DialogosService } from '../../services/dialogos.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Conductores } from './conductores';
import { Conductor, CuentaChofer } from '../../services/conductor.service';

describe('Detección de cuentas de conductores', () => {
  const conductor = { id: 7, dni: '72162260' } as Conductor;
  const cuenta = {
    id: 15,
    dni: '72162260',
    conductorId: null,
    rol: 'CHOFER',
    activo: true,
  } as CuentaChofer;
  const informar = vi.fn(() => Promise.resolve());
  let pagina: Conductores;
  let servicio: { listarCuentas: ReturnType<typeof vi.fn>; crearAcceso: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    informar.mockClear();
    TestBed.configureTestingModule({
      providers: [{ provide: DialogosService, useValue: { informar } }],
    });
    sessionStorage.setItem('rol', 'ADMINISTRADOR');
    servicio = { listarCuentas: vi.fn(() => of([cuenta])), crearAcceso: vi.fn() };
    pagina = TestBed.runInInjectionContext(
      () => new Conductores(servicio as never, new FormBuilder(), {} as never, {} as never),
    );
    pagina.conductores.set([conductor]);
  });
  it('muestra un modal ante un conflicto de DNI de la cuenta', () => {
    pagina['informarDniDuplicado'](
      new HttpErrorResponse({
        status: 409,
        error: { mensaje: 'El DNI ya pertenece a una cuenta administrativa' },
      }),
      true,
    );
    expect(informar).toHaveBeenCalledWith(expect.stringContaining('Este usuario ya existe'));
    expect(informar).toHaveBeenCalledWith(expect.stringContaining('El conductor quedó registrado'));
  });
  it('no confunde una licencia duplicada con un usuario existente', () => {
    pagina['informarDniDuplicado'](
      new HttpErrorResponse({ status: 409, error: { mensaje: 'Licencia duplicada' } }),
    );
    expect(informar).not.toHaveBeenCalled();
  });
  it('explica el correo inválido y permite dejarlo vacío', () => {
    const correo = pagina.conductorForm.controls.correo;
    correo.setValue('ProALV');
    correo.markAsTouched();
    expect(pagina.errorCampo('correo')).toContain('correo válido');
    correo.setValue('');
    expect(pagina.errorCampo('correo')).toBe('');
    expect(correo.valid).toBe(true);
  });
  it('indica únicamente los campos inválidos al intentar guardar', () => {
    pagina.conductorForm.patchValue({
      dni: '87654321',
      telefono: '988521245',
      nombres: 'Lina',
      apellidos: '',
      correo: '',
      numeroLicencia: 'T-124583',
      vencimientoLicencia: '2028-12-20',
    });
    pagina.guardarConductor();
    expect(pagina.mensajeError()).toBe('Ingresa los apellidos del conductor.');
    expect(pagina.errorCampo('correo')).toBe('');
    expect(pagina.guardando()).toBe(false);
  });
  it('mantiene separado el error de operación del error de consulta', () => {
    Object.assign(servicio, { listar: vi.fn(() => of([conductor])) });
    pagina.mensajeError.set('No se creó el acceso');
    pagina.cargarConductores();
    expect(pagina.conductores()).toEqual([conductor]);
    expect(pagina.errorCarga()).toBe('');
    expect(pagina.mensajeError()).toBe('No se creó el acceso');
  });
  it('ignora un segundo envío mientras guarda', () => {
    pagina.guardando.set(true);
    pagina.guardarConductor();
    expect(pagina.mensajeError()).toBe('');
    expect(pagina.guardando()).toBe(true);
  });
  it('reconoce por DNI una cuenta existente sin vínculo al conductor', () => {
    pagina.cargarCuentas();
    expect(pagina.cuentaDe(7)?.id).toBe(15);
    pagina.crearAcceso(conductor);
    expect(servicio.crearAcceso).not.toHaveBeenCalled();
  });
  it('no confunde una cuenta administrativa ni una vinculada a otra persona', () => {
    servicio.listarCuentas.mockReturnValue(of([{ ...cuenta, rol: 'COORDINADOR' }]));
    pagina.cargarCuentas();
    expect(pagina.cuentaDe(7)).toBeNull();
    pagina.cuentasChofer.set([{ ...cuenta, conductorId: 99 }]);
    expect(pagina.cuentaDe(7)).toBeNull();
  });
  it('prioriza la cuenta vinculada sobre la coincidencia de DNI', () => {
    pagina.cuentasChofer.set([cuenta, { ...cuenta, id: 20, conductorId: 7 }]);
    expect(pagina.cuentaDe(7)?.id).toBe(20);
  });
  it('no ofrece crear acceso cuando falla la verificación de cuentas', () => {
    servicio.listarCuentas.mockReturnValue(throwError(() => new Error('Sin conexión')));
    pagina.cargarCuentas();
    pagina.crearAcceso(conductor);
    expect(pagina.errorCuentas()).not.toBe('');
    expect(servicio.crearAcceso).not.toHaveBeenCalled();
  });
});
