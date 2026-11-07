import { formatearDuracionMinutos } from '../../services/formato-duracion';
import { COLORES_SERVICIO } from '../../services/colores-estados';
import { Component, input, signal, computed, inject, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
import { Mantenimiento } from '../../services/mantenimiento.service';
import { EncuestaServicioResponse } from '../../services/encuesta-servicio.service';
import { calcularIndicadores, minutos, promedio } from './indicadores-calculo';
interface Validacion {
  id: number;
  servicioId: number;
  estado: string;
}
@Component({
  selector: 'app-indicadores-operativos',
  imports: [],
  templateUrl: './indicadores.html',
  styleUrl: './indicadores.scss',
})
export class Indicadores implements OnInit {
  readonly servicios = input.required<ServicioTransporte[]>();
  readonly desde = input('');
  readonly hasta = input('');
  readonly ruta = input('TODAS');
  readonly chofer = input('TODOS');
  readonly estado = input('TODOS');
  readonly vista = input<'general' | 'chofer'>('general');
  readonly ahora = Date.now();
  readonly mantenimientos = signal<Mantenimiento[] | null>(null);
  readonly encuestas = signal<EncuestaServicioResponse[] | null>(null);
  readonly validaciones = signal<Validacion[] | null>(null);
  readonly errores = signal<string[]>([]);
  readonly pendientes = signal(3);
  private http = inject(HttpClient);
  private destroy = inject(DestroyRef);
  readonly datos = computed(() =>
    calcularIndicadores(
      this.servicios(),
      {
        desde: this.desde(),
        hasta: this.hasta(),
        ruta: this.ruta(),
        chofer: this.chofer(),
        estado: this.estado(),
      },
      this.ahora,
    ),
  );
  readonly mantenimiento = computed(() => {
    const todos = this.mantenimientos();
    if (todos === null) return null;
    const periodo = todos.filter(
      (m) =>
        (!this.desde() || m.fechaReporte.slice(0, 10) >= this.desde()) &&
        (!this.hasta() || m.fechaReporte.slice(0, 10) <= this.hasta()),
    );
    const abiertos = todos.filter((m) => !['CERRADO', 'CANCELADO'].includes(m.estado));
    const cerrados = periodo.filter((m) => m.estado === 'CERRADO');
    const duraciones = cerrados.flatMap((m) => {
      const n = minutos(m.fechaReporte, m.fechaCierre, this.ahora);
      return n === null ? [] : [n];
    });
    const costos = periodo.filter((m) => ['CERRADO', 'REPARADO'].includes(m.estado));
    const conCosto = costos.filter(
      (m) => typeof m.costo === 'number' && Number.isFinite(m.costo) && m.costo >= 0,
    );
    return {
      periodo,
      abiertos,
      criticos: abiertos.filter((m) => m.prioridad === 'CRITICA'),
      horas: promedio(duraciones) === null ? null : promedio(duraciones)! / 60,
      muestra: duraciones.length,
      costo: conCosto.length ? conCosto.reduce((a, m) => a + m.costo!, 0) : null,
      sinCosto: costos.length - conCosto.length,
    };
  });
  readonly calidad = computed(() => {
    const d = this.datos(),
      evaluaciones = this.encuestas(),
      validaciones = this.validaciones();
    const ids = new Set(d.completados.map((s) => s.id));
    const enc =
      evaluaciones === null
        ? null
        : evaluaciones.filter(
            (e) =>
              ids.has(e.servicioId) &&
              [e.puntualidad, e.cumplimiento, e.atencion, e.satisfaccionGeneral].every(
                (n) => Number.isFinite(n) && n >= 1 && n <= 5,
              ),
          );
    const ultimas = new Map<number, Validacion>();
    for (const v of validaciones || [])
      if (!ultimas.has(v.servicioId) || ultimas.get(v.servicioId)!.id < v.id)
        ultimas.set(v.servicioId, v);
    return {
      nota:
        enc === null
          ? null
          : promedio(
              enc.map(
                (e) => (e.puntualidad + e.cumplimiento + e.atencion + e.satisfaccionGeneral) / 4,
              ),
            ),
      evaluados: enc === null ? null : new Set(enc.map((e) => e.servicioId)).size,
      pendientes: validaciones === null ? null : d.completados.filter((s) => !ultimas.has(s.id)),
      observados:
        validaciones === null
          ? null
          : d.completados.filter((s) => ultimas.get(s.id)?.estado === 'OBSERVADO'),
    };
  });
  readonly unidades = computed(() => {
    const grupos = new Map<
      number,
      { placa: string; completados: number; km: number; muestra: number }
    >();
    for (const d of this.datos().detalles) {
      const id = d.s.vehiculo.id;
      const g = grupos.get(id) || { placa: d.s.vehiculo.placa, completados: 0, km: 0, muestra: 0 };
      g.completados++;
      if (d.km !== null) {
        g.km += d.km;
        g.muestra++;
      }
      grupos.set(id, g);
    }
    return [...grupos.values()].sort((a, b) => b.completados - a.completados);
  });
  readonly repetidos = computed(() => {
    const grupos = new Map<string, { placa: string; tipo: string; codigos: string[] }>();
    for (const m of this.mantenimiento()?.periodo || []) {
      if (
        m.estado === 'CANCELADO' ||
        ['OTRO', 'DOCUMENTACION', 'MANTENIMIENTO_PREVENTIVO'].includes(m.tipoFalla)
      )
        continue;
      const k = m.vehiculoId + ':' + m.tipoFalla;
      const g = grupos.get(k) || {
        placa: m.vehiculoPlaca,
        tipo: m.tipoFalla.replaceAll('_', ' '),
        codigos: [],
      };
      g.codigos.push(m.codigo);
      grupos.set(k, g);
    }
    return [...grupos.values()].filter((g) => g.codigos.length > 1);
  });
  readonly medidaUnidad = signal<'viajes' | 'km'>('viajes');
  readonly estadosGrafico = computed(() => [
    {
      nombre: 'Programados',
      valor: this.datos().servicios.filter((s) => s.estado === 'PROGRAMADO').length,
      color: COLORES_SERVICIO.PROGRAMADO,
    },
    {
      nombre: 'En curso',
      valor: this.datos().servicios.filter((s) => s.estado === 'EN_CURSO').length,
      color: COLORES_SERVICIO.EN_CURSO,
    },
    {
      nombre: 'Completados',
      valor: this.datos().completados.length,
      color: COLORES_SERVICIO.COMPLETADO,
    },
    { nombre: 'Cancelados', valor: this.datos().cancelados, color: COLORES_SERVICIO.CANCELADO },
    { nombre: 'Interrumpidos', valor: this.datos().interrumpidos.length, color: '#e99a24' },
  ]);
  readonly tiemposGrafico = computed(() => {
    const muestra = this.datos().detalles.filter(
      (d) => d.conduccion !== null && d.permanencia !== null,
    );
    return {
      muestra: muestra.length,
      barras: [
        {
          nombre: 'Conducción',
          valor: promedio(muestra.map((d) => d.conduccion!)) ?? 0,
          color: '#007aff',
        },
        {
          nombre: 'Trabajo y espera',
          valor: promedio(muestra.map((d) => d.permanencia!)) ?? 0,
          color: '#a477e8',
        },
      ],
    };
  });
  readonly unidadesGrafico = computed(() =>
    this.unidades()
      .filter((u) => this.medidaUnidad() === 'viajes' || u.muestra > 0)
      .map((u) => ({
        nombre: u.placa,
        valor: this.medidaUnidad() === 'viajes' ? u.completados : u.km,
        color: '#007aff',
      }))
      .sort((a, b) => b.valor - a.valor)
      .slice(0, 8),
  );
  readonly mantenimientoGrafico = computed(() =>
    [
      { nombre: 'Reportado', estado: 'REPORTADO', color: '#e05260' },
      { nombre: 'En evaluación', estado: 'EN_EVALUACION', color: '#007aff' },
      { nombre: 'En mantenimiento', estado: 'EN_MANTENIMIENTO', color: '#e99a24' },
      { nombre: 'Reparado', estado: 'REPARADO', color: '#8b5cf6' },
      { nombre: 'Cerrado', estado: 'CERRADO', color: '#229766' },
      { nombre: 'Cancelado', estado: 'CANCELADO', color: '#8d93a1' },
    ].map((e) => ({
      ...e,
      valor: this.mantenimiento()?.periodo.filter((m) => m.estado === e.estado).length ?? 0,
    })),
  );
  anchoBarra(valor: number, barras: { valor: number }[]) {
    const max = Math.max(...barras.map((b) => b.valor), 1);
    return (valor / max) * 100;
  }
  readonly puntualidadGrafico = computed(() =>
    this.datos().iniciados.length
      ? (this.datos().puntuales.length / this.datos().iniciados.length) * 100
      : 0,
  );
  ngOnInit() {
    if (this.vista() === 'general') {
      this.cargar<Mantenimiento>('mantenimientos', this.mantenimientos);
    } else {
      this.pendientes.update((n) => n - 1);
    }
    this.cargar<EncuestaServicioResponse>('encuestas', this.encuestas);
    this.cargar<Validacion>('validaciones-viaje', this.validaciones);
  }
  private cargar<T>(ruta: string, destino: { set: (v: T[]) => void }) {
    this.http
      .get<T[]>('/api/' + ruta)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (datos) => {
          destino.set(datos);
          this.pendientes.update((n) => n - 1);
        },
        error: () => {
          this.errores.update((e) => [...e, ruta]);
          this.pendientes.update((n) => n - 1);
        },
      });
  }
  readonly tiempo = formatearDuracionMinutos;
  numero(n: number | null, decimales = 1) {
    return n === null
      ? 'Sin datos'
      : n.toLocaleString('es-PE', { maximumFractionDigits: decimales });
  }
  porcentaje(a: number, b: number) {
    return b ? this.numero((a / b) * 100) + ' %' : 'Sin datos';
  }
  readonly nombreChofer = computed(() => {
    const c = this.servicios().find((s) => s.conductor.id === Number(this.chofer()))?.conductor;
    return c ? `${c.nombres} ${c.apellidos}` : 'Conductor';
  });
  readonly salidasGrafico = computed(() =>
    this.datos()
      .salidas.slice()
      .sort((a, b) => Date.parse(a.s.fechaHoraSalida!) - Date.parse(b.s.fechaHoraSalida!))
      .slice(-10)
      .map((d) => ({ nombre: d.s.codigo, valor: d.atraso, color: '#e99a24' })),
  );
  fecha(valor: string | null) {
    if (!valor) return 'Sin registro';
    const d = new Date(valor);
    return Number.isFinite(d.getTime())
      ? d.toLocaleString('es-PE', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        })
      : 'Sin registro';
  }
  atrasoServicio(id: number) {
    const dato = this.datos().salidas.find((d) => d.s.id === id);
    return dato ? this.tiempo(dato.atraso) : 'Sin datos';
  }
}
