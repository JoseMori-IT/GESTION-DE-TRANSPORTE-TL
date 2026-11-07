import { BusquedaLugarService, LugarEncontrado } from '../services/busqueda-lugar.service';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';
import * as L from 'leaflet';

export interface PuntoMapa {
  latitud: number;
  longitud: number;
  nombre: string;
}

@Component({
  selector: 'app-mapa-ubicacion',
  template: `
    @if (editable) {
      <div class="busqueda">
        <label
          >Buscar lugar o dirección
          <input
            #consulta
            type="search"
            placeholder="Ej.: Campo Verde, Pucallpa"
            maxlength="200"
            (input)="cambiarConsulta()"
            (keydown.enter)="
              $event.preventDefault(); $event.stopPropagation(); buscarLugar(consulta.value)
            "
          />
        </label>
        <button type="button" [disabled]="buscando()" (click)="buscarLugar(consulta.value)">
          {{ buscando() ? 'Buscando...' : 'Buscar' }}
        </button>
      </div>
      <small class="credito"
        >Búsqueda:
        <a href="https://photon.komoot.io" target="_blank" rel="noopener noreferrer">Photon</a> ·
        datos de OpenStreetMap</small
      >
      @if (mensajeBusqueda()) {
        <p class="aviso-busqueda" role="status">{{ mensajeBusqueda() }}</p>
      }
      @if (resultados().length) {
        <ul class="resultados" aria-label="Lugares encontrados">
          @for (lugar of resultados(); track $index) {
            <li>
              <button type="button" (click)="elegirLugar(lugar)">{{ lugar.nombre }}</button>
            </li>
          }
        </ul>
      }
    }
    <div
      #mapa
      class="mapa"
      role="region"
      [attr.aria-label]="
        editable ? 'Seleccionar ubicación en el mapa' : 'Vista previa de origen y destino'
      "
    ></div>
    @if (errorMapa()) {
      <p role="status">
        No se pudo cargar el mapa. Revisa tu conexión; puedes ingresar las coordenadas manualmente.
      </p>
    }
  `,
  styles: [
    `
      .busqueda {
        display: flex;
        gap: 8px;
        align-items: end;
        margin-bottom: 5px;
      }
      .busqueda label {
        display: grid;
        gap: 6px;
        flex: 1;
        min-width: 0;
        color: #344c3e;
        font-size: 13px;
        font-weight: 600;
      }
      .busqueda input {
        box-sizing: border-box;
        width: 100%;
        min-width: 0;
        height: 44px;
        padding: 0 12px;
        background: #fdfcf7;
        border: 1px solid #cbd2bf;
        border-radius: 6px;
        font: inherit;
      }
      .busqueda button {
        min-height: 44px;
        padding: 8px 15px;
        color: white;
        background: #204d36;
        border: 0;
        border-radius: 6px;
        font: inherit;
        cursor: pointer;
      }
      .busqueda button:disabled {
        opacity: 0.6;
        cursor: wait;
      }
      .credito {
        display: block;
        margin-bottom: 10px;
        font-size: 11px;
        color: #63705c;
      }
      a {
        color: #204d36;
      }
      .resultados {
        list-style: none;
        padding: 0;
        margin: 0 0 10px;
        max-height: 190px;
        overflow-y: auto;
        border: 1px solid #cbd2bf;
        border-radius: 6px;
      }
      .resultados button {
        display: block;
        width: 100%;
        padding: 10px;
        text-align: left;
        color: #204d36;
        background: #fdfcf7;
        border: 0;
        border-bottom: 1px solid #e5ece7;
        font: inherit;
        font-size: 12px;
        cursor: pointer;
      }
      .resultados button:hover {
        background: #e9eddf;
      }
      .aviso-busqueda {
        color: #344c3e;
        font-size: 12px;
      }
      button:focus-visible,
      input:focus-visible {
        outline: 3px solid #819c42;
        outline-offset: 2px;
      }
      :host {
        display: block;
        min-width: 0;
      }
      .mapa {
        height: 300px;
        border: 1px solid #cbd2bf;
        border-radius: 6px;
        z-index: 0;
      }
      p {
        color: #8b3330;
        font-size: 12px;
      }
    `,
  ],
})
export class MapaUbicacion implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapa', { static: true }) contenedor!: ElementRef<HTMLDivElement>;
  @Input() puntos: PuntoMapa[] = [];
  @Input() editable = false;
  @Input() recorrido: L.LatLngTuple[] = [];
  @Input() recorridos: L.LatLngTuple[][] = [];
  @Input() unirPuntos = true;
  @Output() coordenadas = new EventEmitter<{ latitud: number; longitud: number }>();
  buscando = signal(false);
  resultados = signal<LugarEncontrado[]>([]);
  mensajeBusqueda = signal('');
  private peticion?: AbortController;
  constructor(private buscador: BusquedaLugarService) {}

  cambiarConsulta(): void {
    this.peticion?.abort();
    this.peticion = undefined;
    this.buscando.set(false);
    this.resultados.set([]);
    this.mensajeBusqueda.set('');
  }
  async buscarLugar(consulta: string): Promise<void> {
    if (this.buscando()) return;
    this.cambiarConsulta();
    if (consulta.trim().length < 3) {
      this.mensajeBusqueda.set('Escribe al menos 3 caracteres para buscar.');
      return;
    }
    const peticion = new AbortController();
    this.peticion = peticion;
    this.buscando.set(true);
    const timeout = setTimeout(() => peticion.abort(), 12000);
    try {
      const lugares = await this.buscador.buscar(consulta, peticion.signal);
      if (this.peticion !== peticion) return;
      this.resultados.set(lugares);
      this.mensajeBusqueda.set(
        lugares.length
          ? 'Elige un resultado para ubicarlo en el mapa y completar las coordenadas.'
          : 'No se encontraron lugares. Prueba con el nombre, distrito o región; por ejemplo: Campo Verde, Ucayali.',
      );
    } catch {
      if (this.peticion === peticion)
        this.mensajeBusqueda.set(
          'No se pudo realizar la búsqueda. Revisa tu conexión o vuelve a intentar.',
        );
    } finally {
      clearTimeout(timeout);
      if (this.peticion === peticion) {
        this.buscando.set(false);
        this.peticion = undefined;
      }
    }
  }
  elegirLugar(lugar: LugarEncontrado): void {
    this.mapa?.setView([lugar.latitud, lugar.longitud], 15, { animate: false });
    this.seleccionar(L.latLng(lugar.latitud, lugar.longitud));
    this.resultados.set([]);
    this.mensajeBusqueda.set(
      'Lugar seleccionado. Ajusta el marcador hasta el acceso o punto exacto antes de guardar.',
    );
  }
  errorMapa = signal(false);
  private mapa?: L.Map;
  private firma = '';
  private capas = L.layerGroup();
  private observador?: ResizeObserver;

  ngAfterViewInit(): void {
    this.mapa = L.map(this.contenedor.nativeElement, { scrollWheelZoom: true }).setView(
      [-9.19, -75.0152],
      5,
    );
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    })
      .on('tileerror', () => this.errorMapa.set(true))
      .addTo(this.mapa);
    this.capas.addTo(this.mapa);
    this.mapa.on('click', (e: L.LeafletMouseEvent) => {
      if (this.editable) this.seleccionar(e.latlng);
    });
    if (typeof ResizeObserver !== 'undefined') {
      this.observador = new ResizeObserver(() => this.mapa?.invalidateSize());
      this.observador.observe(this.contenedor.nativeElement);
    }
    this.actualizar();
  }
  ngOnChanges(): void {
    this.actualizar();
  }
  private seleccionar(p: L.LatLng): void {
    this.coordenadas.emit({
      latitud: Number(p.lat.toFixed(7)),
      longitud: Number(p.wrap().lng.toFixed(7)),
    });
  }
  private actualizar(): void {
    if (!this.mapa) return;
    const firma = JSON.stringify([
      this.puntos,
      this.editable,
      this.recorrido,
      this.recorridos,
      this.unirPuntos,
    ]);
    if (firma === this.firma) return;
    this.firma = firma;
    this.capas.clearLayers();
    const puntos = this.puntos.filter(
      (p) =>
        Number.isFinite(p.latitud) &&
        Number.isFinite(p.longitud) &&
        Math.abs(p.latitud) <= 90 &&
        Math.abs(p.longitud) <= 180,
    );
    puntos.forEach((p, i) => {
      if (
        puntos
          .slice(0, i)
          .some((previo) => previo.latitud === p.latitud && previo.longitud === p.longitud)
      )
        return;
      const etiqueta = document.createElement('span');
      etiqueta.textContent = p.nombre;
      const marcador = L.marker([p.latitud, p.longitud], {
        draggable: this.editable,
        icon: L.divIcon({
          className: '',
          html: `<span style="display:grid;place-items:center;width:28px;height:28px;border:2px solid white;border-radius:50%;background:${i === 0 ? '#204d36' : '#a45220'};color:white;font-weight:bold;box-shadow:0 2px 6px #555">${this.editable ? '●' : i < 26 ? String.fromCharCode(65 + i) : i + 1}</span>`,
          iconSize: [28, 28],
          iconAnchor: [14, 14],
        }),
      })
        .bindTooltip(etiqueta)
        .addTo(this.capas);
      marcador.on('dragend', () => this.seleccionar(marcador.getLatLng()));
    });
    if (puntos.length > 1) {
      const posiciones: L.LatLngTuple[] = puntos.map((p) => [p.latitud, p.longitud]);
      const vial = this.recorrido.length > 1;
      if (vial || this.unirPuntos)
        L.polyline(vial ? this.recorrido : posiciones, {
          color: '#204d36',
          dashArray: vial ? undefined : '6 8',
          weight: vial ? 5 : 3,
        }).addTo(this.capas);
      this.recorridos
        .filter((r) => r.length > 1)
        .forEach((r) => L.polyline(r, { color: '#204d36', weight: 5 }).addTo(this.capas));
      this.mapa.fitBounds(
        L.latLngBounds([...posiciones, ...this.recorrido, ...this.recorridos.flat()]),
        { padding: [35, 35], maxZoom: 16, animate: false },
      );
    } else if (puntos.length === 1) {
      this.mapa.setView(
        [puntos[0].latitud, puntos[0].longitud],
        Math.max(this.mapa.getZoom(), 15),
        { animate: false },
      );
    }
  }
  ngOnDestroy(): void {
    this.peticion?.abort();
    this.peticion = undefined;
    this.observador?.disconnect();
    this.mapa?.remove();
  }
}
