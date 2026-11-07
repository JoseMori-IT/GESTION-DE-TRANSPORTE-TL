import { ConfiguracionOperativaService } from '../services/configuracion-operativa.service';
import { inject, OnInit, Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RutaServicio, PlanViajeRequest } from '../services/servicio-transporte.service';
import { Ubicacion } from '../services/ruta.service';
import { CalculoVialViajeComponent, PuntoViajeMapa } from './calculo-vial-viaje';
import { EstimacionTramoViaje } from '../services/servicio-transporte.service';

@Component({
  selector: 'app-itinerario-viaje',
  imports: [FormsModule, CalculoVialViajeComponent],
  template: `
    <section class="plan">
      <header>
        <div>
          <h3>Recorrido del viaje</h3>
          <p>
            Usa la ruta predefinida o agrega paradas antes del destino. El regreso siempre es al
            punto de salida.
          </p>
        </div>
        <span class="badge">Ida y vuelta</span>
      </header>
      <div class="opciones">
        <label
          ><input
            type="checkbox"
            [ngModel]="!conParadas"
            [ngModelOptions]="{ standalone: true }"
            (ngModelChange)="usarSoloRuta($event)"
          />
          Solo ruta predefinida · ida y vuelta</label
        ><button type="button" (click)="agregar()" [disabled]="paradas.length >= 29">
          + Agregar parada
        </button>
      </div>
      <p>
        Salida y regreso: <strong>{{ nombre(inicioId, 'Origen de la ruta') }}</strong>
      </p>
      <ol class="lista-paradas">
        @for (parada of paradas; track $index; let i = $index) {
          <li class="fila-parada">
            <label
              >{{
                i === paradas.length - 1 ? 'Llegada · destino predefinido' : 'Parada ' + (i + 1)
              }}
              @if (i === paradas.length - 1) {
                <strong class="punto-fijo">{{ nombre(parada.ubicacionId, 'Destino') }}</strong>
              } @else {
                <select
                  [(ngModel)]="parada.ubicacionId"
                  [ngModelOptions]="{ standalone: true }"
                  (ngModelChange)="emitir()"
                >
                  <option [ngValue]="0">Selecciona la parada</option>
                  @for (u of ubicaciones; track u.id) {
                    <option [ngValue]="u.id">{{ u.nombre }}</option>
                  }
                </select>
              }
            </label>
            <label
              >Qué hará<select
                [(ngModel)]="parada.actividad"
                [ngModelOptions]="{ standalone: true }"
                (ngModelChange)="emitir()"
              >
                <option value="RECOJO">Recoger</option>
                <option value="ENTREGA">Entregar</option>
                <option value="PASO">Pasar</option>
              </select></label
            >
            <label
              >Producto<select
                [(ngModel)]="parada.producto"
                [ngModelOptions]="{ standalone: true }"
                (ngModelChange)="emitir()"
              >
                @for (p of productos; track p.valor) {
                  <option [value]="p.valor">{{ p.nombre }}</option>
                }
              </select></label
            >
            @if (i < paradas.length - 1) {
              <button
                type="button"
                class="quitar"
                (click)="quitar(i)"
                [attr.aria-label]="'Quitar parada ' + (i + 1)"
              >
                Quitar
              </button>
            }
          </li>
        }
      </ol>
      @if (conParadas) {
        <p>
          Las paradas se realizan en el orden mostrado; el destino predefinido se conserva al final.
        </p>
      }
      <div class="vista" aria-label="Resumen del recorrido">
        <span>{{ nombre(inicioId, 'Inicio') }}</span>
        @for (p of paradas; track $index) {
          <b aria-hidden="true">→</b
          ><span>{{ nombre(p.ubicacionId, 'Parada ' + ($index + 1)) }}</span>
        }
        <b aria-hidden="true">→</b><span>{{ nombre(plantaId, 'Planta de retorno') }}</span>
      </div>
      @if (error) {
        <p class="error" role="status">{{ error }}</p>
      }
      @if (!error) {
        <details class="detalle-km" [open]="conParadas || !estimacionesCatalogo.length">
          <summary>
            {{
              totalEstimado != null
                ? 'Total estimado: ' + totalEstimado + ' km · Ver o recalcular recorrido'
                : 'Completar kilómetros del recorrido'
            }}
          </summary>
          <app-calculo-vial-viaje
            [puntos]="puntosMapa"
            [estimacionesIniciales]="estimacionesCatalogo"
            (cambio)="actualizarEstimaciones($event)"
          />
        </details>
      }
      <small
        >El chofer verá una acción a la vez: salir, llegar a cada parada, regresar y llegar a
        planta.</small
      >
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
        grid-column: 1/-1;
      }
      .plan {
        padding: 20px;
        background: #e9eddf;
        border: 1px solid #daddce;
        border-radius: 6px;
        color: #193d2c;
      }
      input[type='checkbox'] {
        width: 18px;
        min-height: 18px;
        accent-color: #204d36;
      }
      .opciones {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 16px;
        margin: 18px 0;
      }
      .opciones label {
        display: flex;
        align-items: center;
        gap: 9px;
      }
      input[readonly],
      select:disabled {
        background: #e9eddf;
        color: #365239;
        opacity: 1;
      }
      .detalle-km {
        margin: 12px 0;
      }
      .detalle-km summary {
        cursor: pointer;
        font-size: 12px;
        font-weight: 600;
        padding: 12px;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 4px;
      }
      .lista-paradas {
        list-style: none;
        padding: 0;
        margin: 12px 0;
        display: grid;
        gap: 10px;
      }
      .fila-parada {
        display: grid;
        grid-template-columns: minmax(0, 1.3fr) minmax(0, 0.8fr) minmax(0, 1fr) auto;
        gap: 10px;
        align-items: end;
        padding: 12px;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 5px;
      }
      .punto-fijo {
        display: flex;
        align-items: center;
        box-sizing: border-box;
        min-height: 44px;
        padding: 10px;
        background: #e9eddf;
        border-radius: 4px;
        font-weight: 500;
      }
      @media (max-width: 650px) {
        .fila-parada {
          grid-template-columns: 1fr 1fr;
        }
        .fila-parada > label:first-child {
          grid-column: 1/-1;
        }
      }
      header,
      .titulo {
        display: flex;
        align-items: start;
        justify-content: space-between;
        gap: 12px;
      }
      h3 {
        margin: 0;
        font-size: 17px;
        font-weight: 600;
      }
      p,
      small {
        font-size: 12px;
        line-height: 1.6;
        color: #63705c;
      }
      .badge {
        padding: 7px 10px;
        background: #204d36;
        color: white;
        border-radius: 4px;
        font-size: 11px;
        white-space: nowrap;
      }
      .extremos,
      .parada {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
        align-items: start;
      }
      .extremos {
        margin-top: 18px;
      }
      .parada {
        padding: 16px;
        margin-top: 16px;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 5px;
      }
      .titulo {
        grid-column: 1/-1;
        align-items: center;
      }
      label {
        display: grid;
        gap: 6px;
        font-size: 12px;
        font-weight: 600;
      }
      select,
      input {
        box-sizing: border-box;
        width: 100%;
        min-width: 0;
        min-height: 44px;
        padding: 10px;
        background: #fdfcf7;
        border: 1px solid #cbd2bf;
        border-radius: 4px;
        color: #193d2c;
        font: inherit;
        font-weight: 400;
      }
      button {
        min-height: 42px;
        padding: 9px 13px;
        background: #f8f7f1;
        border: 1px solid #cbd2bf;
        border-radius: 4px;
        color: #204d36;
        font: inherit;
        cursor: pointer;
      }
      .quitar {
        min-height: 32px;
        font-size: 11px;
        color: #913e34;
      }
      .agregar {
        margin-top: 14px;
      }
      .vista {
        display: flex;
        align-items: center;
        gap: 9px;
        overflow-x: auto;
        padding: 16px 0;
        margin-top: 6px;
      }
      .vista span {
        flex: 0 0 auto;
        padding: 8px 12px;
        background: #f8f7f1;
        border: 1px solid #cbd2bf;
        border-radius: 4px;
        font-size: 12px;
      }
      .vista b {
        color: #63705c;
      }
      .error {
        color: #913e34;
      }
      button:disabled {
        opacity: 0.5;
        cursor: default;
      }
      button:focus-visible,
      input:focus-visible,
      select:focus-visible {
        outline: 3px solid #819c42;
        outline-offset: 2px;
      }
      @media (max-width: 600px) {
        .extremos,
        .parada {
          grid-template-columns: 1fr;
        }
        .plan {
          padding: 14px;
        }
        header {
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class ItinerarioViajeComponent implements OnChanges, OnInit {
  @Input() rutas: RutaServicio[] = [];
  @Input() ubicaciones: Ubicacion[] = [];
  @Input() rutaInicialId: number | null = null;
  @Input() productoInicial = 'RFF';
  @Output() cambio = new EventEmitter<PlanViajeRequest | null>();
  private readonly configuracion = inject(ConfiguracionOperativaService);
  async ngOnInit() {
    await this.configuracion.cargar();
    this.emitir();
  }
  inicioId = 0;
  plantaId = 0;
  conParadas = false;
  paradas: PlanViajeRequest['paradas'] = [];
  private rutaAplicada: number | null = null;
  private inicializado = false;
  private productoAplicado = '';
  private firmaPuntos = '';
  private estimaciones: EstimacionTramoViaje[] | null = null;
  productos = [
    { valor: 'RFF', nombre: 'Fruto fresco (RFF)' },
    { valor: 'FS', nombre: 'Fruto suelto' },
    { valor: 'ESCOBAJO', nombre: 'Escobajo' },
    { valor: 'FIBRA', nombre: 'Fibra' },
    { valor: 'TORTA_CLARIFICACION', nombre: 'Torta de clarificación' },
    { valor: 'LODO_BIOGAS', nombre: 'Lodo de biogás' },
    { valor: 'CENIZA', nombre: 'Ceniza' },
    { valor: 'RESIDUOS_VARIOS', nombre: 'Residuos varios' },
    { valor: 'SIN_CARGA', nombre: 'Sin carga' },
  ];
  esBase(u: Ubicacion | undefined): boolean {
    return !!u && u.tipo === 'PLANTA';
  }
  get plantas(): Ubicacion[] {
    return this.ubicaciones.filter((u) => this.esBase(u));
  }
  get estimacionesCatalogo(): EstimacionTramoViaje[] {
    if (this.paradas.length !== 1) return [];
    const principal = this.rutas.find((r) => r.id === this.rutaInicialId);
    if (!principal || !principal.distanciaKm || !principal.duracionEstimadaMinutos) return [];
    const ids = [this.inicioId, this.paradas[0].ubicacionId, this.inicioId];
    return ids.slice(1).map((id, i) => {
      const r =
        this.rutas.find(
          (r) =>
            r.origenUbicacion?.id === ids[i] &&
            r.destinoUbicacion?.id === id &&
            r.distanciaKm &&
            r.duracionEstimadaMinutos,
        ) ?? principal;
      return {
        distanciaKm: r.distanciaKm!,
        duracionMinutos: r.duracionEstimadaMinutos!,
        fuente: 'MANUAL',
        recorrido: [],
      };
    });
  }
  ngOnChanges(): void {
    const r = this.rutas.find((r) => r.id === this.rutaInicialId);
    if (r && this.ubicaciones.length && (!this.inicializado || this.rutaAplicada !== r.id)) {
      const origen = this.ubicaciones.find((u) => u.id === r.origenUbicacion?.id);
      const destino = this.ubicaciones.find((u) => u.id === r.destinoUbicacion?.id);
      const recojo = this.esBase(destino) && !this.esBase(origen);
      this.inicioId = (recojo ? destino?.id : origen?.id) ?? 0;
      this.plantaId =
        origen?.tipo === 'PLANTA' ? origen.id : destino?.tipo === 'PLANTA' ? destino.id : 0;
      this.paradas = [
        {
          ubicacionId: (recojo ? origen?.id : destino?.id) ?? 0,
          actividad: recojo ? 'RECOJO' : 'ENTREGA',
          producto: this.productoInicial,
          indicaciones: '',
        },
      ];
      this.plantaId = this.inicioId;
      this.conParadas = false;
      if (!recojo && ['RFF', 'FS'].includes(this.productoInicial))
        this.paradas[0].actividad = 'RECOJO';
      this.rutaAplicada = r.id;
      this.inicializado = true;
    }
    if (this.paradas.length && this.productoAplicado !== this.productoInicial) {
      const principal = this.paradas[this.paradas.length - 1];
      principal.producto = this.productoInicial;
      principal.actividad = ['RFF', 'FS'].includes(this.productoInicial) ? 'RECOJO' : 'ENTREGA';
      this.productoAplicado = this.productoInicial;
    }
    this.emitir();
  }
  nombre(id: number, defecto: string): string {
    return this.ubicaciones.find((u) => u.id === id)?.nombre ?? defecto;
  }
  get error(): string {
    if (this.configuracion.cargando()) return 'Consultando la planta base…';
    if (this.configuracion.error())
      return 'No se pudo validar la planta base. Cierra y vuelve a abrir el formulario.';
    const base = this.configuracion.datos()?.plantaBaseId;
    if (base && (this.inicioId !== base || this.plantaId !== base))
      return (
        'Selecciona una ruta con salida y regreso a ' +
        this.configuracion.datos()?.plantaBaseNombre +
        '.'
      );
    if (!this.inicioId || !this.plantaId || this.paradas.some((p) => !p.ubicacionId))
      return 'Selecciona el inicio, los destinos y la planta de retorno.';
    if (this.plantaId !== this.inicioId) return 'El regreso debe ser a la misma planta de origen.';
    if (!this.plantas.some((p) => p.id === this.inicioId))
      return 'El origen debe ser una planta registrada.';
    const ids = [this.inicioId, ...this.paradas.map((p) => p.ubicacionId), this.plantaId];
    if (ids.some((id, i) => i > 0 && id === ids[i - 1]))
      return 'Dos puntos consecutivos no pueden ser iguales. La planta final ya está incluida como retorno.';
    const r = this.rutas.find((r) => r.id === this.rutaInicialId);
    if (
      r &&
      !ids.some(
        (id, i) =>
          id === r.origenUbicacion?.id && ids.slice(i + 1).includes(r.destinoUbicacion?.id ?? 0),
      )
    )
      return 'El recorrido debe atender el origen y destino de la solicitud en ese orden.';
    return '';
  }
  get puntosMapa(): PuntoViajeMapa[] {
    return [this.inicioId, ...this.paradas.map((p) => p.ubicacionId), this.plantaId].map((id) => {
      const u = this.ubicaciones.find((u) => u.id === id);
      return {
        id,
        nombre: u?.nombre ?? 'Punto sin registrar',
        latitud: u?.latitud ?? null,
        longitud: u?.longitud ?? null,
      };
    });
  }
  actualizarEstimaciones(valor: EstimacionTramoViaje[] | null): void {
    this.estimaciones = valor;
    this.emitir();
  }
  emitir(): void {
    const firma = JSON.stringify(this.puntosMapa);
    if (firma !== this.firmaPuntos) {
      this.firmaPuntos = firma;
      this.estimaciones = null;
    }
    this.cambio.emit(
      this.error || !this.estimaciones
        ? null
        : {
            inicioUbicacionId: this.inicioId,
            plantaRetornoId: this.plantaId,
            paradas: this.paradas.map((p) => ({ ...p })),
            estimaciones: this.estimaciones,
          },
    );
  }
  get totalEstimado(): number | null {
    const es = this.estimaciones ?? this.estimacionesCatalogo;
    return es.length ? Number(es.reduce((n, e) => n + e.distanciaKm, 0).toFixed(2)) : null;
  }
  usarSoloRuta(solo: boolean): void {
    this.conParadas = !solo;
    if (solo && this.paradas.length > 1) this.paradas = this.paradas.slice(-1);
    this.emitir();
  }
  agregar(): void {
    this.conParadas = true;
    this.paradas.splice(Math.max(0, this.paradas.length - 1), 0, {
      ubicacionId: 0,
      actividad: 'ENTREGA',
      producto: 'ESCOBAJO',
      indicaciones: '',
    });
    this.emitir();
  }
  quitar(i: number): void {
    if (i >= 0 && i < this.paradas.length - 1) {
      this.paradas.splice(i, 1);
      this.conParadas = this.paradas.length > 1;
      this.emitir();
    }
  }
}
