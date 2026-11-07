import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnDestroy,
  inject,
  ChangeDetectorRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MapaUbicacion } from './mapa-ubicacion';
import { RecorridoVialService } from '../services/recorrido-vial.service';
import { EstimacionTramoViaje } from '../services/servicio-transporte.service';

export interface PuntoViajeMapa {
  id: number;
  nombre: string;
  latitud: number | null;
  longitud: number | null;
}
interface Segmento {
  nombre: string;
  distanciaKm: number | null;
  duracionMinutos: number | null;
  fuente: 'CARRETERA' | 'MANUAL';
  recorrido: [number, number][];
  nota: string;
}

@Component({
  selector: 'app-calculo-vial-viaje',
  imports: [FormsModule, MapaUbicacion],
  template: `
    <section class="calculo">
      <h4>Recorrido por carretera</h4>
      <p>Calcula la ida, las paradas y el regreso siguiendo los caminos disponibles.</p>
      <button type="button" (click)="calcular()" [disabled]="calculando || puntos.length < 2">
        {{
          calculando
            ? 'Calculando tramo ' + tramoActual + ' de ' + segmentos.length + '...'
            : 'Calcular recorrido por carretera'
        }}
      </button>
      <app-mapa-ubicacion [puntos]="marcadores" [recorridos]="trazados" [unirPuntos]="false" />
      <div class="tramos">
        @for (s of segmentos; track $index; let i = $index) {
          <div class="segmento">
            <strong>{{ i + 1 }}. {{ s.nombre }}</strong>
            <small>{{
              s.fuente === 'CARRETERA' && s.recorrido.length
                ? 'Estimación por carretera'
                : 'Distancia manual'
            }}</small>
            <label
              >Kilómetros<input
                type="number"
                min="0.01"
                step="0.01"
                [ngModel]="s.distanciaKm"
                [ngModelOptions]="{ standalone: true }"
                (ngModelChange)="manual(i, 'distanciaKm', $event)"
                [disabled]="calculando"
            /></label>
            <label
              >Minutos<input
                type="number"
                min="1"
                max="10080"
                step="1"
                [ngModel]="s.duracionMinutos"
                [ngModelOptions]="{ standalone: true }"
                (ngModelChange)="manual(i, 'duracionMinutos', $event)"
                [disabled]="calculando"
            /></label>
            @if (s.nota) {
              <p role="status">{{ s.nota }}</p>
            }
          </div>
        }
      </div>
      <p class="total" role="status">
        {{
          completo
            ? 'Total planificado: ' + totalKm + ' km · ' + totalMinutos + ' min de conducción'
            : 'Calcula el recorrido o completa los kilómetros y minutos de todos los tramos para asignar el viaje.'
        }}
      </p>
      <small
        >La línea del mapa sigue las carreteras encontradas. Los tramos manuales no se unen con
        líneas rectas. El total es estimado y no incluye el tiempo detenido en las paradas.</small
      >
      <small
        >Rutas:
        <a href="https://project-osrm.org/" target="_blank" rel="noopener noreferrer">OSRM</a> /
        FOSSGIS · datos de OpenStreetMap ·
        <a href="https://www.openstreetmap.org/fixthemap" target="_blank" rel="noopener noreferrer"
          >Corregir el mapa</a
        ></small
      >
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .calculo {
        margin-top: 18px;
        padding: 16px;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 6px;
      }
      h4 {
        margin: 0;
        font-size: 15px;
      }
      p,
      small {
        font-size: 12px;
        line-height: 1.6;
        color: #63705c;
      }
      small {
        display: block;
      }
      button {
        min-height: 44px;
        margin: 6px 0 16px;
        padding: 10px 14px;
        background: #204d36;
        color: white;
        border: 0;
        border-radius: 4px;
        font: inherit;
        cursor: pointer;
      }
      button:disabled {
        opacity: 0.6;
        cursor: wait;
      }
      .tramos {
        display: grid;
        gap: 10px;
        margin-top: 14px;
      }
      .segmento {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 8px 12px;
        padding: 12px;
        border: 1px solid #daddce;
        border-radius: 4px;
      }
      .segmento > strong,
      .segmento > small,
      .segmento > p {
        grid-column: 1/-1;
      }
      .segmento > strong {
        font-size: 12px;
      }
      .segmento > p {
        margin: 0;
        color: #913e34;
      }
      label {
        display: grid;
        gap: 5px;
        font-size: 12px;
      }
      input {
        box-sizing: border-box;
        width: 100%;
        min-height: 42px;
        padding: 9px;
        border: 1px solid #cbd2bf;
        border-radius: 4px;
        background: #fdfcf7;
        color: #193d2c;
        font: inherit;
      }
      .total {
        font-weight: 600;
        color: #204d36;
      }
      a {
        color: #204d36;
      }
      button:focus-visible,
      input:focus-visible {
        outline: 3px solid #819c42;
        outline-offset: 2px;
      }
    `,
  ],
})
export class CalculoVialViajeComponent implements OnChanges, OnDestroy {
  @Input() puntos: PuntoViajeMapa[] = [];
  @Input() estimacionesIniciales: EstimacionTramoViaje[] = [];
  @Output() cambio = new EventEmitter<EstimacionTramoViaje[] | null>();
  private readonly vial = inject(RecorridoVialService);
  private readonly cdr = inject(ChangeDetectorRef);
  private firma = '';
  private peticion?: AbortController;
  segmentos: Segmento[] = [];
  calculando = false;
  tramoActual = 0;
  ngOnChanges(): void {
    const firma = JSON.stringify(this.puntos);
    if (firma === this.firma) return;
    this.firma = firma;
    this.peticion?.abort();
    this.peticion = undefined;
    this.calculando = false;
    this.segmentos = this.puntos
      .slice(1)
      .map((p, i) => ({
        nombre: this.puntos[i].nombre + ' → ' + p.nombre,
        distanciaKm: null,
        duracionMinutos: null,
        fuente: 'MANUAL',
        recorrido: [],
        nota: '',
      }));
    if (this.estimacionesIniciales.length === this.segmentos.length)
      this.segmentos.forEach((s, i) => {
        const e = this.estimacionesIniciales[i];
        Object.assign(s, {
          distanciaKm: e.distanciaKm,
          duracionMinutos: e.duracionMinutos,
          fuente: 'MANUAL',
          nota: 'Estimación del catálogo. Si no hay ruta inversa registrada, el regreso usa una aproximación igual a la ida. Puedes recalcular por carretera.',
        });
      });
    this.emitir();
  }
  get marcadores() {
    return this.puntos.flatMap((p) =>
      p.latitud != null && p.longitud != null
        ? [{ nombre: p.nombre, latitud: p.latitud, longitud: p.longitud }]
        : [],
    );
  }
  get trazados() {
    return this.segmentos.filter((s) => s.fuente === 'CARRETERA').map((s) => s.recorrido);
  }
  get completo() {
    return (
      this.segmentos.length > 0 &&
      this.segmentos.every(
        (s) =>
          s.distanciaKm != null &&
          Number.isFinite(s.distanciaKm) &&
          s.distanciaKm >= 0.01 &&
          s.distanciaKm <= 99999999.99 &&
          s.duracionMinutos != null &&
          Number.isInteger(s.duracionMinutos) &&
          s.duracionMinutos >= 1 &&
          s.duracionMinutos <= 10080,
      )
    );
  }
  get totalKm() {
    return Number(this.segmentos.reduce((n, s) => n + (s.distanciaKm ?? 0), 0).toFixed(2));
  }
  get totalMinutos() {
    return this.segmentos.reduce((n, s) => n + (s.duracionMinutos ?? 0), 0);
  }
  private emitir() {
    this.cambio.emit(
      this.completo && !this.calculando
        ? this.segmentos.map((s) => ({
            distanciaKm: s.distanciaKm!,
            duracionMinutos: s.duracionMinutos!,
            fuente: s.fuente,
            recorrido: s.recorrido,
          }))
        : null,
    );
  }
  manual(i: number, campo: 'distanciaKm' | 'duracionMinutos', valor: number | null): void {
    if (this.calculando) return;
    const s = this.segmentos[i];
    s[campo] = valor;
    if (campo === 'distanciaKm') {
      s.fuente = 'MANUAL';
      s.recorrido = [];
      s.nota = 'Distancia ingresada manualmente; no hay trazado vial para este tramo.';
    } else
      s.nota =
        s.fuente === 'CARRETERA'
          ? 'Tiempo de conducción ajustado manualmente.'
          : 'Completa la distancia y duración aproximadas de este tramo.';
    this.emitir();
  }
  async calcular(): Promise<void> {
    if (this.calculando) return;
    const peticion = new AbortController();
    this.peticion = peticion;
    this.calculando = true;
    this.emitir();
    try {
      for (let i = 0; i < this.segmentos.length; i++) {
        if (this.peticion !== peticion || peticion.signal.aborted) return;
        this.tramoActual = i + 1;
        this.cdr.markForCheck();
        const a = this.puntos[i],
          b = this.puntos[i + 1],
          s = this.segmentos[i];
        if (a.latitud == null || a.longitud == null || b.latitud == null || b.longitud == null) {
          s.nota =
            'Faltan coordenadas. Complétalas en Ubicaciones o ingresa kilómetros y minutos manualmente.';
          continue;
        }
        const timer = setTimeout(() => peticion.abort(), 15000);
        try {
          const r = await this.vial.calcular(
            { latitud: a.latitud, longitud: a.longitud },
            { latitud: b.latitud, longitud: b.longitud },
            peticion.signal,
          );
          if (this.peticion !== peticion || peticion.signal.aborted) return;
          Object.assign(s, {
            distanciaKm: r.distanciaKm,
            duracionMinutos: r.duracionMinutos,
            recorrido: r.trayectoria,
            fuente: 'CARRETERA',
            nota: '',
          });
        } catch {
          if (this.peticion !== peticion) return;
          Object.assign(s, {
            distanciaKm: null,
            duracionMinutos: null,
            recorrido: [],
            fuente: 'MANUAL',
            nota: 'No se pudo obtener este camino. Ingresa los kilómetros y minutos aproximados.',
          });
          if (peticion.signal.aborted) {
            for (const pendiente of this.segmentos.slice(i + 1))
              pendiente.nota =
                'Consulta interrumpida. Vuelve a calcular o completa este tramo manualmente.';
            break;
          }
        } finally {
          clearTimeout(timer);
          this.cdr.markForCheck();
        }
      }
    } finally {
      if (this.peticion === peticion) {
        this.calculando = false;
        this.peticion = undefined;
        this.emitir();
      }
    }
  }
  ngOnDestroy() {
    this.peticion?.abort();
    this.peticion = undefined;
  }
}
