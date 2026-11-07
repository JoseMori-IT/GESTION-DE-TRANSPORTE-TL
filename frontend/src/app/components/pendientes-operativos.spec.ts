import { describe, it, expect, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { pendientesOperativos, PendientesOperativosComponent } from './pendientes-operativos';
import { Mantenimiento } from '../services/mantenimiento.service';
import { SolicitudTransporte } from '../services/solicitud-transporte';
import { ServicioTransporte } from '../services/servicio-transporte.service';
const m = (id: number, estado: string) =>
  ({ id, estado, vehiculoPlaca: 'ABC-123', titulo: 'Frenos', prioridad: 'ALTA' }) as Mantenimiento;
const s = (id: number, estado: string) =>
  ({ id, estado, codigo: 'SOL-' + id, solicitanteNombre: 'Prueba' }) as SolicitudTransporte;
describe('Pendientes operativos', () => {
  it('incluye etapas abiertas y excluye procesos terminados', () => {
    const p = pendientesOperativos(
      [m(1, 'REPORTADO'), m(2, 'REPARADO'), m(3, 'CERRADO')],
      [s(4, 'EN_REVISION'), s(5, 'APROBADA'), s(6, 'COMPLETADA')],
      [],
    );
    expect(p.map((x) => x.clave).sort()).toEqual(['man-1', 'man-2', 'sol-4', 'sol-5']);
    expect(p.find((x) => x.id === 5)?.accion).toBe('Programar servicio');
  });
  it('prioriza una avería y evita repetir su mantenimiento vinculado', () => {
    const viaje = {
      id: 8,
      codigo: 'SER-8',
      estado: 'EN_CURSO',
      vehiculo: { placa: 'ABC-123' },
      incidencias: [{ estado: 'REPORTADA', mantenimientoId: 1 }],
    } as ServicioTransporte;
    const p = pendientesOperativos([m(1, 'REPORTADO')], [], [viaje]);
    expect(p).toHaveLength(1);
    expect(p[0].ruta).toBe('/servicios');
    viaje.estado = 'INTERRUMPIDO';
    expect(pendientesOperativos([m(1, 'REPORTADO')], [], [viaje])[0].ruta).toBe('/mantenimiento');
  });
  it('refresca los avisos, navega al registro y no modifica datos al abrir', () => {
    const navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [PendientesOperativosComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
      ],
    });
    const f = TestBed.createComponent(PendientesOperativosComponent);
    f.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const resolver = (estado: string) => {
      http.expectOne('/api/mantenimientos').flush([m(1, estado)]);
      http.expectOne('/api/solicitudes').flush([]);
      http.expectOne('/api/servicios').flush([]);
    };
    resolver('REPORTADO');
    expect(f.componentInstance.items()).toHaveLength(1);
    f.componentInstance.abrir(f.componentInstance.items()[0]);
    expect(navigate).toHaveBeenCalledWith(['/mantenimiento'], { queryParams: { atender: 1 } });
    f.componentInstance.cargar();
    resolver('CERRADO');
    expect(f.componentInstance.items()).toEqual([]);
    http.expectNone((r) => r.method !== 'GET');
    http.verify();
    f.destroy();
    TestBed.resetTestingModule();
  });
  it('un error de consulta no se presenta como ausencia de pendientes', () => {
    TestBed.configureTestingModule({
      imports: [PendientesOperativosComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    const f = TestBed.createComponent(PendientesOperativosComponent);
    f.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const m = http.expectOne('/api/mantenimientos');
    http.expectOne('/api/solicitudes').flush([]);
    http.expectOne('/api/servicios').flush([]);
    m.flush({}, { status: 500, statusText: 'Error' });
    f.componentInstance.abierto.set(true);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('No se pudieron consultar');
    expect(f.nativeElement.textContent).not.toContain('No hay procesos pendientes');
    http.verify();
    f.destroy();
    TestBed.resetTestingModule();
  });
});
