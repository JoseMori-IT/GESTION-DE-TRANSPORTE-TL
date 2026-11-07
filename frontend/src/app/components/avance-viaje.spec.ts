import { TestBed } from '@angular/core/testing';
import { ConfiguracionOperativaService } from '../services/configuracion-operativa.service';
import { ResumenSolicitudViajeComponent } from './resumen-solicitud-viaje';
import { afterEach, describe, expect, it } from 'vitest';
import { AvanceViajeComponent, tramosConPendientes } from './avance-viaje';
import { ServicioTransporte, TramoServicio } from '../services/servicio-transporte.service';
import { MarcacionPendiente } from '../services/marcacion-servicio.service';
import { ItinerarioViajeComponent } from './itinerario-viaje';

const viaje = {
  id: 7,
  tramos: [
    {
      id: 1,
      orden: 1,
      origen: 'Tulumayo',
      destino: 'Pepito',
      salidaReal: '2026-10-23T08:00:00',
      llegadaReal: null,
    },
    { id: 2, orden: 2, origen: 'Pepito', destino: 'Tulumayo', salidaReal: null, llegadaReal: null },
  ] as TramoServicio[],
} as ServicioTransporte;
function crearEditor() {
  TestBed.configureTestingModule({
    providers: [
      {
        provide: ConfiguracionOperativaService,
        useValue: {
          cargando: () => false,
          error: () => '',
          datos: () => null,
          cargar: async () => {},
        },
      },
    ],
  });
  return TestBed.runInInjectionContext(() => new ItinerarioViajeComponent());
}
describe('Avance y paradas sin conexión', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('incorpora las marcas locales sin modificar la copia confirmada', () => {
    const marcas = [
      {
        servicioId: 7,
        tramoOrden: 1,
        tipo: 'LLEGADA_DESTINO',
        fechaHoraDispositivo: '2026-10-23T09:00:00',
      },
      {
        servicioId: 7,
        tramoOrden: 2,
        tipo: 'SALIDA_IDA',
        fechaHoraDispositivo: '2026-10-23T09:15:00',
      },
    ] as MarcacionPendiente[];
    const tramos = tramosConPendientes(viaje, marcas);
    expect(tramos[0].llegadaReal).toBe('2026-10-23T09:00:00');
    expect(tramos[1].salidaReal).toBe('2026-10-23T09:15:00');
    expect(viaje.tramos![0].llegadaReal).toBeNull();
    const barra = new AvanceViajeComponent();
    barra.servicio = viaje;
    barra.pendientes = marcas;
    expect(barra.porcentaje).toBe(50);
    expect(barra.sinSincronizar).toBe(true);
  });
  it('no cuenta eventos de otro viaje y llega al 100% solo con todas las llegadas', () => {
    const barra = new AvanceViajeComponent();
    barra.servicio = viaje;
    barra.pendientes = [
      {
        servicioId: 99,
        tramoOrden: 1,
        tipo: 'LLEGADA_DESTINO',
        fechaHoraDispositivo: '2026-10-23T09:00:00',
      },
    ] as MarcacionPendiente[];
    expect(barra.porcentaje).toBe(0);
    barra.pendientes = [1, 2].map((orden) => ({
      servicioId: 7,
      tramoOrden: orden,
      tipo: 'LLEGADA_DESTINO',
      fechaHoraDispositivo: '2026-10-23T10:00:00',
    })) as MarcacionPendiente[];
    expect(barra.porcentaje).toBe(100);
  });
  it('planifica una ida y vuelta eligiendo puntos, sin registrar rutas por tramo', () => {
    const editor = crearEditor();
    editor.rutas = [{ id: 1, origenUbicacion: { id: 1 }, destinoUbicacion: { id: 2 } }] as never;
    editor.ubicaciones = [
      { id: 1, nombre: 'Tulumayo', tipo: 'PLANTA' },
      { id: 2, nombre: 'Pepito', tipo: 'PARCELA' },
      { id: 3, nombre: 'Acopio', tipo: 'ACOPIO' },
    ] as never;
    editor.rutaInicialId = 1;
    editor.ngOnChanges();
    expect(editor.inicioId).toBe(1);
    expect(editor.plantaId).toBe(1);
    expect(editor.paradas[0].ubicacionId).toBe(2);
    expect(editor.error).toBe('');
    editor.agregar();
    expect(editor.error).not.toBe('');
    expect(editor.paradas[1].ubicacionId).toBe(2);
    editor.paradas[0].ubicacionId = 3;
    editor.emitir();
    expect(editor.error).toBe('');
    editor.quitar(0);
    expect(editor.paradas.length).toBe(1);
    expect(editor.paradas[0].ubicacionId).toBe(2);
    editor.agregar();
    editor.usarSoloRuta(true);
    expect(editor.paradas.length).toBe(1);
    expect(editor.conParadas).toBe(false);
  });
  it('para recojo propone salir de planta, visitar al productor y regresar', () => {
    const editor = crearEditor();
    editor.rutas = [{ id: 1, origenUbicacion: { id: 2 }, destinoUbicacion: { id: 1 } }] as never;
    editor.ubicaciones = [
      { id: 1, nombre: 'Planta', tipo: 'PLANTA' },
      { id: 2, nombre: 'Productor', tipo: 'PARCELA' },
    ] as never;
    editor.rutaInicialId = 1;
    editor.ngOnChanges();
    expect(editor.inicioId).toBe(1);
    expect(editor.paradas[0].actividad).toBe('RECOJO');
    expect(editor.plantaId).toBe(1);
    expect(editor.error).toBe('');
    editor.plantaId = 2;
    expect(editor.error).toContain('planta');
  });
  it('compara la lectura final con la salida e incluye las lecturas sin conexión', () => {
    const barra = new AvanceViajeComponent();
    barra.servicio = { ...viaje, distanciaEstimadaKm: 100 };
    barra.pendientes = [
      { servicioId: 7, tramoOrden: 1, tipo: 'SALIDA_IDA', odometroKm: 12500 },
      { servicioId: 7, tramoOrden: 2, tipo: 'LLEGADA_DESTINO', odometroKm: 12615.5 },
    ] as MarcacionPendiente[];
    expect(barra.distanciaReal).toBe(115.5);
    expect(barra.diferencia).toBe(15.5);
    barra.pendientes = barra.pendientes.slice(0, 1);
    expect(barra.distanciaReal).toBeNull();
  });

  it('separa 30 minutos de ida, trabajo en parada y 30 minutos de regreso', () => {
    const barra = new AvanceViajeComponent();
    barra.servicio = {
      ...viaje,
      tramos: [
        {
          ...viaje.tramos![0],
          salidaReal: '2026-10-23T08:00:00',
          llegadaReal: '2026-10-23T08:30:00',
        },
        {
          ...viaje.tramos![1],
          salidaReal: '2026-10-23T09:30:00',
          llegadaReal: '2026-10-23T10:00:00',
        },
      ],
    };
    expect(barra.tiemposReales).toEqual({ conduccion: 60, paradas: 60, completo: true });
    expect(barra.permanencia(0)).toBe(60);
    expect(barra.duracionTotalReal).toBe(120);
    barra.servicio = {
      ...barra.servicio,
      tramos: barra.servicio.tramos!.map((t, i) => (i === 1 ? { ...t, llegadaReal: null } : t)),
    };
    expect(barra.duracionTotalReal).toBeNull();
  });
  it('el resumen suma la ida y regreso y no usa un total antiguo como un solo tramo', () => {
    const resumen = new ResumenSolicitudViajeComponent();
    resumen.solicitud = {
      resumenRecorrido: 'Tulumayo → Neshuya → Tulumayo',
      duracionEstimadaMinutos: 30,
      planViaje: {
        paradas: [{ ubicacionId: 2 }],
        estimaciones: [{ duracionMinutos: 30 }, { duracionMinutos: 30 }],
      },
    } as never;
    expect(resumen.totalConduccion).toBe(60);
    expect(resumen.tramos[0].nombre).toBe('Tulumayo → Neshuya');
    expect(resumen.tramos[1].nombre).toContain('(regreso)');
  });

  it('el camión avanza solo por las marcas de salida y llegada y se detiene en las paradas', () => {
    const c = new AvanceViajeComponent();
    c.servicio = {
      ...viaje,
      estado: 'PROGRAMADO',
      tramos: viaje.tramos!.map((t) => ({ ...t, salidaReal: null, llegadaReal: null })),
    };
    expect(c.posicionCamion).toBe(0);
    expect(c.estadoActual).toBe('Pendiente de salida');
    c.servicio.tramos![0].salidaReal = '2026-10-23T08:00:00';
    expect(c.posicionCamion).toBe(25);
    expect(c.enCamino).toBe(true);
    expect(c.porcentaje).toBe(0);
    c.servicio.tramos![0].llegadaReal = '2026-10-23T08:30:00';
    expect(c.posicionCamion).toBe(50);
    expect(c.enCamino).toBe(false);
    expect(c.estadoActual).toContain('En parada');
    c.servicio.tramos![1].salidaReal = '2026-10-23T09:00:00';
    expect(c.posicionCamion).toBe(75);
    c.servicio.tramos![1].llegadaReal = '2026-10-23T09:30:00';
    expect(c.posicionCamion).toBe(100);
    expect(c.estadoActual).toContain('Viaje finalizado');
    c.servicio.estado = 'CANCELADO';
    expect(c.enCamino).toBe(false);
    expect(c.estadoActual).toBe('Viaje cancelado');
  });

  it('el plan muestra permanencia y duración reales cuando recibe el viaje completado', () => {
    const c = new ResumenSolicitudViajeComponent();
    c.solicitud = { planViaje: { paradas: [{ ubicacionId: 2 }] } } as never;
    c.viaje = {
      ...viaje,
      estado: 'COMPLETADO',
      tramos: [
        {
          ...viaje.tramos![0],
          salidaReal: '2026-10-23T08:00:00',
          llegadaReal: '2026-10-23T08:30:00',
        },
        {
          ...viaje.tramos![1],
          salidaReal: '2026-10-23T09:30:00',
          llegadaReal: '2026-10-23T10:00:00',
        },
      ],
    };
    expect(c.totalPermanencia).toBe(60);
    expect(c.permanencia(0)).toBe(60);
    expect(c.trasladoReal(0)).toBe(30);
    expect(c.duracionReal).toBe(120);
    expect(c.permanenciasCompletas).toBe(true);
    c.viaje.tramos![1].salidaReal = null;
    expect(c.totalPermanencia).toBeNull();
    expect(c.sinDatos).toBe('Sin marcaciones completas');
  });
});

describe('Auxilio con tres acciones sin conexión', () => {
  it('la recogida habilita la llegada a planta sin otra salida y no cambia viajes normales', () => {
    const marcas = [
      {
        servicioId: 7,
        tramoOrden: 1,
        tipo: 'LLEGADA_DESTINO',
        fechaHoraDispositivo: '2026-10-23T09:00:00',
      },
    ] as MarcacionPendiente[];
    const aux = { ...viaje, servicioAuxiliadoId: 3 };
    const tramos = tramosConPendientes(aux, marcas);
    expect(tramos[1].salidaReal).toBe('2026-10-23T09:00:00');
    expect(tramos[1].llegadaReal).toBeNull();
    expect(tramosConPendientes(viaje, marcas)[1].salidaReal).toBeNull();
    expect(aux.tramos![1].salidaReal).toBeNull();
  });
});

it('un nombre de planta no reemplaza el tipo registrado', () => {
  const e = crearEditor();
  expect(e.esBase({ tipo: 'OTRO', nombre: 'Planta Tulumayo' } as never)).toBe(false);
  TestBed.resetTestingModule();
});
