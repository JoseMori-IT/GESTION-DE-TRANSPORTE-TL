import { ConfiguracionOperativaService } from '../../services/configuracion-operativa.service';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Solicitudes } from './solicitudes';
import {
  SolicitudTransporteService,
  SolicitudTransporte,
} from '../../services/solicitud-transporte';
import { SesionService } from '../../services/sesion.service';
import { PlanViajeRequest } from '../../services/servicio-transporte.service';
const ruta = {
  id: 1,
  codigo: 'RUT-001',
  nombre: 'Tulumayo - Neshuya',
  origen: 'Tulumayo',
  destino: 'Neshuya',
  estado: 'ACTIVA',
  origenUbicacion: { id: 1 },
  destinoUbicacion: { id: 2 },
};
const ubicaciones = [
  { id: 1, nombre: 'Tulumayo', tipo: 'PLANTA', estado: 'ACTIVA' },
  { id: 2, nombre: 'Neshuya', tipo: 'PARCELA', estado: 'ACTIVA' },
  { id: 3, nombre: 'Curimaná', tipo: 'ACOPIO', estado: 'ACTIVA' },
];
const plan: PlanViajeRequest = {
  inicioUbicacionId: 1,
  plantaRetornoId: 1,
  paradas: [
    { ubicacionId: 3, actividad: 'ENTREGA', producto: 'ESCOBAJO', indicaciones: 'Descargar' },
    { ubicacionId: 2, actividad: 'RECOJO', producto: 'RFF', indicaciones: '' },
  ],
  estimaciones: [
    { distanciaKm: 20, duracionMinutos: 30, fuente: 'MANUAL', recorrido: [] },
    { distanciaKm: 30, duracionMinutos: 40, fuente: 'MANUAL', recorrido: [] },
    { distanciaKm: 45, duracionMinutos: 55, fuente: 'MANUAL', recorrido: [] },
  ],
};
const solicitud = {
  id: 1,
  codigo: 'SOL-001',
  estado: 'APROBADA',
  rutaId: 1,
  productoTransportado: 'RFF',
  planViaje: plan,
  resumenRecorrido: 'Tulumayo → Curimaná → Neshuya → Tulumayo',
  distanciaEstimadaKm: 95,
  duracionEstimadaMinutos: 125,
} as SolicitudTransporte;
describe('Solicitud define el recorrido y programación lo conserva', () => {
  let api: {
    buscarPorId: ReturnType<typeof vi.fn>;
    cambiarEstado: ReturnType<typeof vi.fn>;
    listar: ReturnType<typeof vi.fn>;
    registrar: ReturnType<typeof vi.fn>;
    programar: ReturnType<typeof vi.fn>;
    completarRecorrido: ReturnType<typeof vi.fn>;
  };
  beforeEach(() => {
    sessionStorage.setItem('rol', 'ADMINISTRADOR');
    api = {
      buscarPorId: vi.fn(() =>
        of({
          ...solicitud,
          fechaRequerida: '2099-10-23T10:00',
          tipoSolicitud: 'RECOJO_RFF',
          servicioId: null,
        }),
      ),
      cambiarEstado: vi.fn(() => of({ ...solicitud, estado: 'EN_REVISION' })),
      listar: vi.fn(() => of([])),
      registrar: vi.fn(() => of(solicitud)),
      programar: vi.fn(() => of(solicitud)),
      completarRecorrido: vi.fn(() => of({ ...solicitud, estado: 'EN_REVISION' })),
    };
    TestBed.configureTestingModule({
      imports: [Solicitudes],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ConfiguracionOperativaService,
          useValue: {
            cargando: () => false,
            error: () => '',
            datos: () => null,
            cargar: async () => {},
          },
        },
        { provide: Router, useValue: {} },
        { provide: SesionService, useValue: {} },
        { provide: SolicitudTransporteService, useValue: api },
      ],
    });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });
  function catalogo() {
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/rutas').flush([ruta]);
    http.expectOne('/api/ubicaciones').flush(ubicaciones);
  }
  function recursos() {
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/vehiculos')
      .flush([{ id: 1, placa: 'ABC-123', tipo: 'CAMION', estado: 'DISPONIBLE' }]);
    http
      .expectOne('/api/conductores')
      .flush([{ id: 2, estado: 'DISPONIBLE', rolTripulacion: 'CHOFER' }]);
    catalogo();
    http.expectOne('/api/servicios').flush([]);
  }
  it('selecciona RFF y FS automáticamente y permite otros productos manuales', () => {
    const pagina = TestBed.createComponent(Solicitudes).componentInstance;
    const f = pagina.formularioSolicitud.controls;
    f.tipoSolicitud.setValue('RECOJO_RFF');
    pagina.ajustarCargaPorTipo();
    expect(f.productoTransportado.value).toBe('RFF');
    expect(f.productoTransportado.disabled).toBe(true);
    f.tipoSolicitud.setValue('RECOJO_FS');
    pagina.ajustarCargaPorTipo();
    expect(f.productoTransportado.value).toBe('FS');
    expect(pagina.formularioSolicitud.getRawValue().productoTransportado).toBe('FS');
    f.tipoSolicitud.setValue('MOVIMIENTO_INTERNO');
    pagina.ajustarCargaPorTipo();
    expect(f.productoTransportado.enabled).toBe(true);
    f.productoTransportado.setValue('FIBRA');
    expect(f.productoTransportado.value).toBe('FIBRA');
    f.tipoSolicitud.setValue('RECOJO_RFF');
    pagina.ajustarCargaPorTipo();
    expect(f.productoTransportado.value).toBe('RFF');
  });
  it('el editor de paradas aparece al seleccionar la ruta de la nueva solicitud', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirFormulario();
    catalogo();
    pagina.formularioSolicitud.controls.rutaId.setValue(1);
    pagina.seleccionarRutaSolicitud();
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('app-itinerario-viaje')).not.toBeNull();
    expect(html.textContent).toContain('Salida y regreso:');
    expect(html.querySelector('.new-route')).toBeNull();
    expect(pagina.itinerarioSolicitud()).toBeNull();
    expect(html.textContent).toContain('Solo ruta predefinida');
  });
  it('envía todas las paradas en la solicitud y bloquea el registro sin estimaciones', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    const pagina = fixture.componentInstance;
    pagina.formularioSolicitud.patchValue({
      rutaId: 1,
      solicitanteNombre: 'Prueba',
      areaSolicitante: 'Operaciones',
      origen: 'Tulumayo',
      destino: 'Neshuya',
      fechaRequerida: '2099-10-23T10:00',
    });
    pagina.guardarSolicitud();
    expect(api.registrar).not.toHaveBeenCalled();
    pagina.itinerarioSolicitud.set(plan);
    pagina.guardarSolicitud();
    expect(api.registrar).toHaveBeenCalledWith(
      expect.objectContaining({ rutaId: 1, planViaje: plan, cantidadEstimadaToneladas: null }),
    );
  });
  it('la programación muestra el recorrido aprobado sin editor y no lo reenvía para cambiarlo', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirProgramacion(solicitud);
    recursos();
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('app-itinerario-viaje')).toBeNull();
    expect(html.textContent).toContain('Tulumayo → Curimaná → Neshuya → Tulumayo');
    expect(pagina.itinerario()).toEqual(plan);
    pagina.formularioProgramacion.patchValue({
      vehiculoId: 1,
      conductorId: 2,
      fechaHoraSalida: '2099-10-23T10:00',
    });
    pagina.programarSolicitud();
    expect(api.programar).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ rutaId: 1, requiereRetorno: true }),
    );
    expect(api.programar.mock.calls[0][1].planViaje).toBeUndefined();
  });
  it('una solicitud antigua debe completar el recorrido y enviarlo a revisión', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirProgramacion({ ...solicitud, planViaje: null });
    catalogo();
    fixture.detectChanges();
    expect(pagina.mostrarProgramacion()).toBe(false);
    expect(pagina.solicitudSinRecorrido()?.id).toBe(1);
    pagina.itinerarioSolicitud.set(plan);
    pagina.guardarRecorridoAnterior();
    expect(api.completarRecorrido).toHaveBeenCalledWith(1, 1, plan);
    expect(pagina.solicitudSinRecorrido()).toBeNull();
  });
  it('solo ruta predefinida usa una planta registrada y los kilómetros del catálogo', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirFormulario();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/rutas').flush([{ ...ruta, distanciaKm: 40, duracionEstimadaMinutos: 50 }]);
    http
      .expectOne('/api/ubicaciones')
      .flush(
        ubicaciones.map((u) =>
          u.id === 1 ? { ...u, nombre: 'Planta Tulumayo', tipo: 'PLANTA' } : u,
        ),
      );
    pagina.formularioSolicitud.controls.rutaId.setValue(1);
    pagina.seleccionarRutaSolicitud();
    fixture.detectChanges();
    fixture.detectChanges();
    expect(pagina.itinerarioSolicitud()?.estimaciones?.map((e) => e.distanciaKm)).toEqual([40, 40]);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain(
      'El origen debe ser una planta registrada.',
    );
  });

  it('abre una revisión con detalle antes de cambiar el estado y permite aprobar', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    const pagina = fixture.componentInstance;
    const revisar = {
      ...solicitud,
      estado: 'SOLICITADA',
      solicitanteNombre: 'Juan',
      areaSolicitante: 'Operaciones',
      fechaRequerida: '2099-10-23T10:00',
      prioridad: 'NORMAL',
      tipoSolicitud: 'RECOJO_RFF',
      cantidadEstimadaToneladas: null,
    } as SolicitudTransporte;
    pagina.pasarARevision(revisar);
    fixture.detectChanges();
    expect(api.cambiarEstado).not.toHaveBeenCalled();
    const dialog = fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement;
    expect(dialog.textContent).toContain('Juan');
    expect(dialog.textContent).toContain('Tulumayo → Curimaná → Neshuya → Tulumayo');
    expect(dialog.textContent).toContain('No indicada');
    pagina.confirmarRevision();
    expect(api.cambiarEstado).toHaveBeenCalledWith(1, 'EN_REVISION', null);
    expect(pagina.solicitudRevision()).toBeNull();
    pagina.aprobar({ ...revisar, estado: 'EN_REVISION' });
    expect(pagina.aprobandoSolicitud()).toBe(true);
    fixture.detectChanges();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.approval-banner')?.textContent,
    ).toContain('Confirmación de autorización');
    api.cambiarEstado.mockReturnValue(of({ ...revisar, estado: 'APROBADA' }));
    pagina.confirmarRevision();
    expect(api.cambiarEstado).toHaveBeenCalledWith(1, 'APROBADA', null);
    expect(pagina.solicitudRevision()).toBeNull();
  });
  it('conserva abierta la revisión y muestra el error cuando falla el cambio de estado', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    const pagina = fixture.componentInstance;
    pagina.pasarARevision({ ...solicitud, estado: 'SOLICITADA' });
    api.cambiarEstado.mockReturnValue(
      throwError(() => ({ status: 409, error: { mensaje: 'Solicitud modificada' } })),
    );
    pagina.confirmarRevision();
    expect(pagina.solicitudRevision()).not.toBeNull();
    expect(pagina.errorRevision()).toContain('Solicitud modificada');
  });

  it('visualiza la solicitud actualizada sin cambiar su estado y sin servicio asignado', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    const pagina = fixture.componentInstance;
    pagina.visualizarSolicitud(solicitud);
    fixture.detectChanges();
    expect(api.buscarPorId).toHaveBeenCalledWith(1);
    expect(api.cambiarEstado).not.toHaveBeenCalled();
    expect(pagina.viajeConsulta()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Pendiente de programación',
    );
    pagina.cerrarConsulta();
    expect(pagina.solicitudConsulta()).toBeNull();
  });
  it('carga el viaje vinculado y muestra su avance confirmado', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    const pagina = fixture.componentInstance;
    api.buscarPorId.mockReturnValue(
      of({
        ...solicitud,
        servicioId: 9,
        fechaRequerida: '2099-10-23T10:00',
        tipoSolicitud: 'RECOJO_RFF',
      }),
    );
    pagina.visualizarSolicitud(solicitud);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/servicios/9')
      .flush({
        id: 9,
        codigo: 'SER-001',
        estado: 'EN_CURSO',
        vehiculo: { placa: 'ABC-123' },
        conductor: { nombres: 'Juan' },
        tramos: [
          {
            id: 1,
            orden: 1,
            origen: 'Tulumayo',
            destino: 'Neshuya',
            salidaReal: '2099-10-23T10:00',
            llegadaReal: null,
          },
        ],
      });
    fixture.detectChanges();
    expect(pagina.viajeConsulta()?.estado).toBe('EN_CURSO');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-avance-viaje')).not.toBeNull();
    expect(api.cambiarEstado).not.toHaveBeenCalled();
  });

  it('confirma el código generado, primer nombre y apellido y recorrido; solo cierra con la X', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirProgramacion(solicitud);
    recursos();
    pagina.choferes.set([{ id: 2, nombres: '  Juan Carlos ', apellidos: ' Pérez López' }] as never);
    pagina.formularioProgramacion.patchValue({
      vehiculoId: 1,
      conductorId: 2,
      fechaHoraSalida: '2099-10-23T10:00',
    });
    api.programar.mockReturnValue(
      of({ ...solicitud, estado: 'PROGRAMADA', servicioCodigo: 'SER-GENERADO-123' }),
    );
    pagina.programarSolicitud();
    fixture.detectChanges();
    expect(pagina.viajeAsignado()).toEqual({
      codigo: 'SER-GENERADO-123',
      chofer: 'Juan Pérez',
      unidad: 'ABC-123',
      recorrido: solicitud.resumenRecorrido,
    });
    const modal = fixture.nativeElement.querySelector(
      'app-confirmacion-viaje dialog',
    ) as HTMLDialogElement;
    expect(modal.textContent).toContain('VIAJE ASIGNADO CORRECTAMENTE');
    expect(modal.textContent).toContain('ABC-123');
    expect(modal.textContent).toContain('SER-GENERADO-123');
    expect(modal.textContent).toContain('Juan Pérez');
    expect(modal.textContent).toContain('Tulumayo → Curimaná → Neshuya → Tulumayo');
    expect(modal.querySelectorAll('button').length).toBe(1);
    expect(modal.dispatchEvent(new Event('cancel', { cancelable: true }))).toBe(false);
    modal.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(pagina.viajeAsignado()).not.toBeNull();
    (modal.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(pagina.viajeAsignado()).toBeNull();
    expect(fixture.nativeElement.querySelector('app-confirmacion-viaje')).toBeNull();
  });
  it('no muestra confirmación si falla la programación ni duplica la petición mientras espera', () => {
    const fixture = TestBed.createComponent(Solicitudes);
    fixture.detectChanges();
    const pagina = fixture.componentInstance;
    pagina.abrirProgramacion(solicitud);
    recursos();
    pagina.formularioProgramacion.patchValue({
      vehiculoId: 1,
      conductorId: 2,
      fechaHoraSalida: '2099-10-23T10:00',
    });
    const respuesta = new Subject<SolicitudTransporte>();
    api.programar.mockReturnValue(respuesta);
    pagina.programarSolicitud();
    pagina.programarSolicitud();
    expect(api.programar).toHaveBeenCalledTimes(1);
    expect(pagina.viajeAsignado()).toBeNull();
    respuesta.error({ error: { mensaje: 'Unidad ocupada' } });
    fixture.detectChanges();
    expect(pagina.viajeAsignado()).toBeNull();
    expect(pagina.mostrarProgramacion()).toBe(true);
    expect(pagina.mensajeFormulario()).toContain('Unidad ocupada');
  });
});
