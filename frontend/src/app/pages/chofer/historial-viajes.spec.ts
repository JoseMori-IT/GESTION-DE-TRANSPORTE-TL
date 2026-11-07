import { Component, Input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, it, expect } from 'vitest';
import { HistorialViajesComponent } from './historial-viajes';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
@Component({ selector: 'app-avance-viaje', template: 'Detalle del recorrido' })
class AvancePrueba {
  @Input() servicio: unknown;
}
@Component({ selector: 'app-incidencia-viaje', template: '' })
class IncidenciaPrueba {
  @Input() servicio: unknown;
  @Input() chofer = false;
}
function preparar() {
  TestBed.configureTestingModule({ imports: [HistorialViajesComponent] });
  TestBed.overrideComponent(HistorialViajesComponent, {
    set: { imports: [AvancePrueba, IncidenciaPrueba] },
  });
  const fixture = TestBed.createComponent(HistorialViajesComponent);
  const viaje = {
    id: 1,
    codigo: 'SER-001',
    estado: 'COMPLETADO',
    origenDescripcion: 'Planta',
    destinoDescripcion: 'Planta',
    vehiculo: { placa: 'ABC-123' },
    productoTransportado: 'RFF',
    fechaHoraSalida: '2026-11-07T10:00:00',
    fechaHoraSalidaReal: null,
    fechaHoraLlegadaReal: null,
  } as ServicioTransporte;
  fixture.componentRef.setInput('viajes', [viaje]);
  fixture.detectChanges();
  return { fixture, viaje };
}
describe('Historial del chofer', () => {
  it('muestra una tarjeta sin cargar el recorrido al abrir el historial', () => {
    const { fixture } = preparar();
    expect(fixture.nativeElement.querySelector('.history-card')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-avance-viaje')).toBeNull();
  });
  it('permite abrir y cerrar el detalle con el mismo botón', () => {
    const { fixture } = preparar();
    const boton = fixture.nativeElement.querySelector(
      '.history-detail-button',
    ) as HTMLButtonElement;
    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('app-avance-viaje')).toBeTruthy();
    boton.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-avance-viaje')).toBeNull();
  });
  it('solicita consultar evidencias sin modificar el viaje completado', () => {
    const { fixture, viaje } = preparar();
    let seleccionado: ServicioTransporte | undefined;
    fixture.componentInstance.evidencias.subscribe((v) => (seleccionado = v));
    fixture.componentInstance.expandidoId.set(1);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.history-evidence-button').click();
    expect(seleccionado).toBe(viaje);
    expect(viaje.estado).toBe('COMPLETADO');
  });
});
