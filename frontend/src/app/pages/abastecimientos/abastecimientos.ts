import { ConfiguracionOperativaService } from '../../services/configuracion-operativa.service';
import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  viewChild,
  ElementRef,
  afterRenderEffect,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Vehiculo } from '../../services/vehiculo.service';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
import { calcularConsumo } from '../reportes/consumo-calculo';
export interface Abastecimiento {
  id: number;
  versionRegistro: number;
  vehiculoId: number;
  placa: string;
  fecha: string;
  galones: number;
  odometroKm: number | null;
  motivoSinOdometro: string | null;
  grifo: string;
  comprobante: string | null;
  observaciones: string | null;
  registradoPor: string;
  fechaRegistro: string;
  anulado: boolean;
  motivoAnulacion: string | null;
  anuladoPor: string | null;
  fechaAnulacion: string | null;
}
export function resumirAbastecimientos(
  filas: Abastecimiento[],
  desde: string,
  hasta: string,
  unidad: string,
) {
  const seleccion = filas.filter(
    (x) =>
      (unidad === 'TODAS' || x.vehiculoId === Number(unidad)) &&
      (!desde || x.fecha.slice(0, 10) >= desde) &&
      (!hasta || x.fecha.slice(0, 10) <= hasta),
  );
  const vigentes = seleccion.filter((x) => !x.anulado);
  return { seleccion, vigentes, galones: vigentes.reduce((n, x) => n + x.galones, 0) };
}
function fechaLocal() {
  const d = new Date();
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}T${z(d.getHours())}:${z(d.getMinutes())}`;
}
@Component({
  selector: 'app-abastecimientos',
  imports: [FormsModule, RouterLink],
  templateUrl: './abastecimientos.html',
  styleUrl: './abastecimientos.scss',
})
export class Abastecimientos implements OnInit {
  private dialogo = viewChild<ElementRef<HTMLDialogElement>>('dialogo');
  constructor() {
    afterRenderEffect(() => {
      const d = this.dialogo()?.nativeElement;
      if (d && !d.open) {
        if (typeof d.showModal === 'function') d.showModal();
        else d.setAttribute('open', '');
      }
    });
  }
  private http = inject(HttpClient);
  readonly registros = signal<Abastecimiento[]>([]);
  readonly vehiculos = signal<Vehiculo[]>([]);
  readonly viajes = signal<ServicioTransporte[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly errorFormulario = signal('');
  readonly exito = signal('');
  readonly modal = signal(false);
  readonly anulando = signal<Abastecimiento | null>(null);
  readonly desde = signal('');
  readonly hasta = signal('');
  readonly unidad = signal('TODAS');
  readonly configuracion = inject(ConfiguracionOperativaService);
  readonly puedeRegistrar = ['MASTER', 'COORDINADOR', 'ADMINISTRADOR', 'OPERADOR'].includes(
    sessionStorage.getItem('rol') || '',
  );
  readonly puedeAnular = ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(
    sessionStorage.getItem('rol') || '',
  );
  readonly filtrosInvalidos = computed(
    () => !!this.desde() && !!this.hasta() && this.desde() > this.hasta(),
  );
  readonly resumen = computed(() =>
    resumirAbastecimientos(this.registros(), this.desde(), this.hasta(), this.unidad()),
  );
  readonly estimado = computed(() =>
    calcularConsumo(
      this.viajes().filter(
        (s) => this.unidad() === 'TODAS' || s.vehiculo.id === Number(this.unidad()),
      ),
      { desde: this.desde(), hasta: this.hasta(), estado: 'TODOS', ruta: 'TODAS', chofer: 'TODOS' },
      this.configuracion.referencia(),
    ),
  );
  readonly porUnidad = computed(() => {
    const mapa = new Map<
      number,
      { id: number; placa: string; cargado: number; estimado: number }
    >();
    for (const x of this.resumen().vigentes) {
      const g = mapa.get(x.vehiculoId) || {
        id: x.vehiculoId,
        placa: x.placa,
        cargado: 0,
        estimado: 0,
      };
      g.cargado += x.galones;
      mapa.set(g.id, g);
    }
    for (const x of this.estimado().porUnidad) {
      const id = Number(x.clave),
        g = mapa.get(id) || { id, placa: x.nombre, cargado: 0, estimado: 0 };
      g.estimado = x.galones;
      mapa.set(id, g);
    }
    return [...mapa.values()].sort((a, b) => b.cargado - a.cargado);
  });
  readonly maxBarra = computed(() =>
    Math.max(1, ...this.porUnidad().flatMap((x) => [x.cargado, x.estimado])),
  );
  readonly unidadesRegistro = computed(() =>
    this.vehiculos().filter((v) => !['BAJA_DEFINITIVA', 'INACTIVO'].includes(v.estado)),
  );
  datos = this.nuevo();
  motivoAnulacion = '';
  sinOdometro = false;
  nuevo() {
    return {
      vehiculoId: null as number | null,
      fecha: fechaLocal(),
      galones: null as number | null,
      odometroKm: null as number | null,
      motivoSinOdometro: '',
      grifo: '',
      comprobante: '',
      observaciones: '',
      eventoUuid: crypto.randomUUID(),
    };
  }
  ngOnInit() {
    this.cargar();
  }
  cargar() {
    if (this.cargando()) return;
    void this.configuracion.cargar();
    this.cargando.set(true);
    this.error.set('');
    forkJoin({
      a: this.http.get<Abastecimiento[]>('/api/abastecimientos'),
      v: this.http.get<Vehiculo[]>('/api/vehiculos'),
      s: this.http.get<ServicioTransporte[]>('/api/servicios'),
    }).subscribe({
      next: ({ a, v, s }) => {
        this.registros.set(a);
        this.vehiculos.set(v);
        this.viajes.set(s);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudieron consultar los abastecimientos. Reintenta.');
      },
    });
  }
  abrir() {
    if (!this.puedeRegistrar) return;
    this.datos = this.nuevo();
    this.sinOdometro = false;
    this.errorFormulario.set('');
    this.modal.set(true);
  }
  seleccionarUnidad() {
    const v = this.vehiculos().find((v) => v.id === this.datos.vehiculoId);
    this.datos.odometroKm = v?.odometroKm ?? null;
    this.sinOdometro = v?.estadoOdometro === 'AVERIADO';
  }
  cerrar() {
    if (this.guardando()) return;
    this.modal.set(false);
    this.anulando.set(null);
    this.errorFormulario.set('');
  }
  estimacionUnidad(id: number) {
    return this.estimado().porUnidad.find((x) => Number(x.clave) === id)?.galones ?? null;
  }
  maxFecha() {
    return fechaLocal();
  }
  guardar() {
    if (this.guardando() || !this.puedeRegistrar) return;
    this.errorFormulario.set('');
    if (
      !this.datos.vehiculoId ||
      !this.datos.fecha ||
      !Number.isFinite(new Date(this.datos.fecha).getTime()) ||
      new Date(this.datos.fecha).getTime() > Date.now() ||
      !this.datos.galones ||
      this.datos.galones <= 0 ||
      !this.datos.grifo.trim()
    ) {
      this.errorFormulario.set('Completa unidad, fecha no futura, galones y grifo.');
      return;
    }
    if (this.sinOdometro && this.datos.motivoSinOdometro.trim().length < 5) {
      this.errorFormulario.set('Explica por qué no hay lectura de odómetro.');
      return;
    }
    if (!this.sinOdometro && this.datos.odometroKm === null) {
      this.errorFormulario.set('Indica el odómetro o marca que no se dispone de lectura.');
      return;
    }
    this.guardando.set(true);
    const datos = {
      ...this.datos,
      fecha: this.datos.fecha + ':00',
      odometroKm: this.sinOdometro ? null : this.datos.odometroKm,
      motivoSinOdometro: this.sinOdometro ? this.datos.motivoSinOdometro : null,
    };
    this.http.post<Abastecimiento>('/api/abastecimientos', datos).subscribe({
      next: (a) => {
        this.registros.update((f) =>
          [a, ...f.filter((x) => x.id !== a.id)].sort(
            (a, b) => b.fecha.localeCompare(a.fecha) || b.id - a.id,
          ),
        );
        this.guardando.set(false);
        this.modal.set(false);
        this.exito.set('Abastecimiento registrado correctamente.');
      },
      error: (e) => {
        this.guardando.set(false);
        this.errorFormulario.set(
          e.error?.mensaje ||
            e.error?.detail ||
            e.error?.message ||
            'No se pudo registrar. Los datos se conservan para reintentar.',
        );
      },
    });
  }
  abrirAnulacion(a: Abastecimiento) {
    if (!this.puedeAnular) return;
    this.anulando.set(a);
    this.motivoAnulacion = '';
    this.errorFormulario.set('');
  }
  confirmarAnulacion() {
    const a = this.anulando();
    if (!a || this.guardando() || !this.puedeAnular || this.motivoAnulacion.trim().length < 5)
      return;
    this.guardando.set(true);
    this.http
      .post<Abastecimiento>(`/api/abastecimientos/${a.id}/anular`, {
        version: a.versionRegistro,
        motivo: this.motivoAnulacion,
      })
      .subscribe({
        next: (r) => {
          this.registros.update((f) => f.map((x) => (x.id === r.id ? r : x)));
          this.guardando.set(false);
          this.anulando.set(null);
          this.exito.set('Registro anulado. Se conserva su trazabilidad.');
        },
        error: (e) => {
          this.guardando.set(false);
          this.errorFormulario.set(
            e.error?.mensaje ||
              e.error?.detail ||
              e.error?.message ||
              'No se pudo anular. Actualiza y reintenta.',
          );
        },
      });
  }
  numero(n: number | null) {
    return n === null ? 'Sin datos' : n.toLocaleString('es-PE', { maximumFractionDigits: 3 });
  }
  fecha(v: string) {
    return new Date(v).toLocaleString('es-PE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
