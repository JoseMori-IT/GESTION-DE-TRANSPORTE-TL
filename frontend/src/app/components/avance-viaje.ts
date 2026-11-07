import { evaluarDistancia } from '../services/distancia-verificable';
import { minutosAveria } from '../services/tiempos-averia';
import { MapaUbicacion, PuntoMapa } from './mapa-ubicacion';
import { Component, Input } from '@angular/core';
import { ServicioTransporte, TramoServicio } from '../services/servicio-transporte.service';
import { MarcacionPendiente } from '../services/marcacion-servicio.service';

export function tramosConPendientes(
  servicio: ServicioTransporte,
  pendientes: MarcacionPendiente[],
): TramoServicio[] {
  const tramos = (servicio.tramos ?? []).map((t) => {
    const eventos = pendientes.filter(
      (e) => e.servicioId === servicio.id && e.tramoOrden === t.orden,
    );
    const salida = eventos.find((e) => e.tipo === 'SALIDA_IDA'),
      llegada = eventos.find((e) => e.tipo === 'LLEGADA_DESTINO');
    return {
      ...t,
      odometroNoDisponibleSalida: t.odometroNoDisponibleSalida ?? salida?.odometroNoDisponible,
      odometroNoDisponibleLlegada: t.odometroNoDisponibleLlegada ?? llegada?.odometroNoDisponible,
      motivoOdometroSalida: t.motivoOdometroSalida ?? salida?.motivoOdometro,
      motivoOdometroLlegada: t.motivoOdometroLlegada ?? llegada?.motivoOdometro,
      odometroSalidaKm:
        t.odometroSalidaKm ?? eventos.find((e) => e.tipo === 'SALIDA_IDA')?.odometroKm ?? null,
      odometroLlegadaKm:
        t.odometroLlegadaKm ??
        eventos.find((e) => e.tipo === 'LLEGADA_DESTINO')?.odometroKm ??
        null,
      salidaReal:
        t.salidaReal || eventos.find((e) => e.tipo === 'SALIDA_IDA')?.fechaHoraDispositivo || null,
      llegadaReal:
        t.llegadaReal ||
        eventos.find((e) => e.tipo === 'LLEGADA_DESTINO')?.fechaHoraDispositivo ||
        null,
    };
  });
  if (
    servicio.servicioAuxiliadoId &&
    tramos.length === 2 &&
    tramos[0].llegadaReal &&
    !tramos[1].salidaReal
  )
    tramos[1].salidaReal = tramos[0].llegadaReal;
  return tramos;
}
@Component({
  selector: 'app-avance-viaje',
  imports: [MapaUbicacion],
  template: `
    @if (tramos.length) {
      <section class="avance" aria-label="Avance del viaje">
        <header>
          <div>
            <small>Seguimiento del recorrido</small><strong>{{ estadoActual }}</strong>
          </div>
          <span class="contador">{{ completados }} / {{ tramos.length }} tramos completados</span>
        </header>
        <div class="carretera" aria-label="Posición del camión según marcaciones">
          <div class="via">
            <div class="via-completa" [style.width.%]="posicionCamion"></div>
            <div
              class="camion"
              [class.en-marcha]="enCamino"
              [style.left.%]="posicionCamion"
              aria-hidden="true"
            >
              <svg viewBox="0 0 64 42" width="58" height="40">
                <rect x="4" y="6" width="33" height="24" rx="4" fill="currentColor" />
                <path d="M39 14h12l9 11v7H39z" fill="currentColor" />
                <path d="M43 17h6l6 8H43z" fill="#e5edda" />
                <path d="M7 32h50" stroke="#153e2c" stroke-width="4" />
                <circle cx="16" cy="33" r="7" fill="#153e2c" />
                <circle cx="49" cy="33" r="7" fill="#153e2c" />
                <circle cx="16" cy="33" r="3" fill="#f8f7f1" />
                <circle cx="49" cy="33" r="3" fill="#f8f7f1" />
                <path d="M0 16h-4M0 23h-7" stroke="currentColor" stroke-width="2" />
              </svg>
            </div>
            @for (t of tramos; track t.orden; let i = $index) {
              @if (tramos.length <= 8 || i === tramos.length - 1 || i === completados - 1) {
                <span
                  class="hito"
                  [class.listo]="t.llegadaReal"
                  [style.left.%]="((i + 1) * 100) / tramos.length"
                  [attr.title]="t.destino"
                  >{{ t.llegadaReal ? '✓' : i + 1 }}</span
                >
              }
            }
            <span
              class="hito inicio"
              [class.listo]="tramos[0].salidaReal"
              style="left:0"
              title="Salida"
              >A</span
            >
          </div>
          <div class="extremos">
            <span>{{ tramos[0].origen }}</span
            ><span>Regreso · {{ tramos[tramos.length - 1].destino }}</span>
          </div>
        </div>
        <p class="estado-ubicacion">
          <span class="estado-punto" [class.movimiento]="enCamino"></span>{{ estadoActual }}
          @if (sinSincronizar) {
            <span class="local">Pendiente de sincronizar</span>
          }
        </p>
        @if (averiaActiva; as averia) {
          <div class="detalle-averia" role="status">
            <strong>{{ averia.descripcion }}</strong
            ><span>Ubicación reportada: {{ averia.lugar }}</span
            ><small
              >Reportada: {{ fecha(averia.fechaReporte) }}
              @if (averia.tecnico) {
                · Técnico: {{ averia.tecnico }}
              }
              @if (averia.servicioAuxilioId) {
                · Auxilio #{{ averia.servicioAuxilioId }}
              }
            </small>
          </div>
        }
        <div
          class="medidor"
          role="progressbar"
          aria-label="Tramos completados"
          [attr.aria-valuenow]="porcentaje"
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <span [style.width.%]="porcentaje"></span>
        </div>
        <ol>
          @for (t of tramos; track t.orden; let i = $index) {
            <li
              [class.hecho]="t.llegadaReal"
              [class.actual]="
                i === (enCamino || !completados ? indiceActivo : completados - 1) &&
                servicio.estado !== 'CANCELADO'
              "
            >
              <div class="titulo-parada">
                <span class="numero-parada">{{ t.llegadaReal ? '✓' : i + 1 }}</span
                ><b>{{ i === tramos.length - 1 ? 'Regreso al origen' : 'Parada ' + (i + 1) }}</b>
              </div>
              <strong>{{ t.destino }}</strong
              ><span
                >{{ actividad(t.actividad) }} ·
                {{
                  t.productoTransportado === 'SIN_CARGA' ? 'Sin carga' : t.productoTransportado
                }}</span
              >
              <small class="estado-tramo">{{
                t.llegadaReal
                  ? 'Llegada registrada'
                  : t.salidaReal
                    ? averiaActiva
                      ? 'Detenido por avería antes de llegar'
                      : servicio.estado === 'INTERRUMPIDO'
                        ? 'No llegó · viaje interrumpido'
                        : 'En camino a este punto'
                    : 'Pendiente'
              }}</small>
              @if (permanencia(i) != null) {
                <small>Permanencia: {{ permanencia(i) }} min · trabajo y espera</small>
              }
              @if (t.indicaciones) {
                <small>{{ t.indicaciones }}</small>
              }
              @if (t.llegadaReal || t.salidaReal) {
                <small>{{
                  t.llegadaReal
                    ? 'Llegada: ' + fecha(t.llegadaReal)
                    : 'Salida: ' + fecha(t.salidaReal)
                }}</small>
              }
            </li>
          }
        </ol>
        @if (servicio.distanciaEstimadaKm != null) {
          <p>
            <strong>Total planificado: {{ servicio.distanciaEstimadaKm }} km</strong> ·
            {{ servicio.duracionEstimadaMinutos }} min de conducción. No corresponde al kilometraje
            real del odómetro.
          </p>
        }
        @if (
          tramos[0].odometroNoDisponibleSalida ||
          tramos[tramos.length - 1].odometroNoDisponibleLlegada
        ) {
          <p>
            <strong>Distancia estimada por ruta · odómetro averiado</strong><br />{{
              tramos[0].motivoOdometroSalida || tramos[tramos.length - 1].motivoOdometroLlegada
            }}
          </p>
        }
        @if (odometroSalida != null) {
          <p>
            Odómetro de salida: <strong>{{ odometroSalida }} km</strong>
            @if (odometroFinal != null) {
              · Odómetro final: <strong>{{ odometroFinal }} km</strong>
            }
          </p>
        }
        @if (distanciaReal != null) {
          <p>
            <strong>Recorrido real: {{ distanciaReal }} km</strong>
            @if (diferencia != null) {
              · Diferencia con lo planificado: {{ diferencia! > 0 ? '+' : '' }}{{ diferencia }} km
            }
          </p>
        }
        @if (tiemposReales; as tiempos) {
          <div class="tiempos-reales">
            <div>
              <small>Traslados registrados</small><strong>{{ tiempos.conduccion }} min</strong>
            </div>
            <div>
              <small>Permanencia en paradas</small><strong>{{ tiempos.paradas }} min</strong>
            </div>
            <div>
              <small>Duración total del viaje</small
              ><strong>{{
                duracionTotalReal != null
                  ? duracionTotalReal + ' min'
                  : servicio.estado === 'INTERRUMPIDO'
                    ? 'Interrumpido'
                    : 'En curso'
              }}</strong>
            </div>
          </div>
          <p>
            Los traslados se miden entre salida y llegada. La permanencia se mide entre llegada y
            nueva salida e incluye trabajo y espera.
            {{
              tiempos.completo
                ? 'Viaje finalizado.'
                : 'Tiempos parciales: solo intervalos ya cerrados.'
            }}
          </p>
        }
        @if (hayRecorrido) {
          <details (toggle)="mostrarMapa = $any($event.target).open">
            <summary>Ver recorrido por carretera y kilómetros por tramo</summary>
            @if (mostrarMapa) {
              <app-mapa-ubicacion
                [puntos]="puntosMapa"
                [recorridos]="recorridos"
                [unirPuntos]="false"
              />
              @for (t of tramos; track t.orden) {
                <p>
                  {{ t.origen }} → {{ t.destino }}: {{ t.distanciaEstimadaKm }} km ·
                  {{
                    t.fuenteDistancia === 'CARRETERA' ? 'Por carretera' : 'Manual, sin trazado vial'
                  }}
                </p>
              }
            }
          </details>
        }
        <p>
          {{ servicio.estado === 'CANCELADO' ? 'Viaje cancelado. ' : ''
          }}{{
            sinSincronizar
              ? 'Hay marcaciones guardadas en este dispositivo, pendientes de sincronizar.'
              : 'Avance según las marcaciones recibidas; no representa GPS en vivo.'
          }}
          Última marcación: {{ ultimaFecha ? fecha(ultimaFecha) : 'Sin marcaciones' }}.
        </p>
      </section>
    }
  `,
  styles: [
    `
      .detalle-averia {
        display: grid;
        gap: 6px;
        margin: 12px 0;
        padding: 14px;
        background: #fff8ed;
        border: 1px solid #f2cf96;
        border-radius: 10px;
        overflow-wrap: anywhere;
      }
      .detalle-averia span {
        font-size: 12px;
      }
      .detalle-averia strong {
        color: #8a4b00;
      }
      :host {
        display: block;
        min-width: 0;
      }
      .avance {
        padding: 20px;
        margin: 16px 0;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 10px;
        color: #193d2c;
        white-space: normal;
        font:
          12px 'Palmas Poppins',
          sans-serif;
      }
      header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
      header > div {
        display: grid;
        gap: 5px;
      }
      header > div > strong {
        font-size: 15px;
      }
      .contador {
        background: #e9eddf;
        border-radius: 20px;
        padding: 7px 11px;
        font-size: 10px;
        color: #526845;
      }
      .carretera {
        padding: 44px 28px 0;
      }
      .via {
        position: relative;
        height: 16px;
        background: #d9dfcf;
        border: 3px solid #d9dfcf;
        border-radius: 12px;
      }
      .via:after {
        content: '';
        position: absolute;
        inset: 5px 0;
        border-top: 2px dashed #fdfcf7;
        pointer-events: none;
      }
      .via-completa {
        height: 100%;
        background: #719255;
        border-radius: 10px;
        transition: width 0.8s ease;
      }
      .camion {
        position: absolute;
        bottom: 12px;
        transform: translateX(-50%);
        color: #204d36;
        z-index: 3;
        transition: left 0.8s ease;
        filter: drop-shadow(0 3px 2px #193d2c22);
      }
      .camion.en-marcha svg {
        animation: motor 1.2s ease-in-out infinite;
      }
      .hito {
        position: absolute;
        top: 50%;
        transform: translate(-50%, -50%);
        display: grid;
        place-items: center;
        width: 23px;
        height: 23px;
        border: 3px solid #f8f7f1;
        border-radius: 50%;
        background: #c4ceb6;
        color: #4b6140;
        z-index: 2;
        font-size: 9px;
        font-weight: 600;
      }
      .hito.listo {
        background: #204d36;
        color: white;
      }
      .extremos {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        margin: 18px -18px 0;
        color: #63705c;
        font-size: 10px;
      }
      .extremos span {
        max-width: 48%;
        overflow-wrap: anywhere;
      }
      .extremos span:last-child {
        text-align: right;
      }
      .estado-ubicacion {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 20px;
        font-size: 12px;
        color: #204d36;
      }
      .estado-punto {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #809568;
      }
      .estado-punto.movimiento {
        background: #204d36;
        box-shadow: 0 0 0 4px #e2ead7;
      }
      .local {
        font-size: 10px;
        padding: 4px 8px;
        background: #f4ead0;
        border-radius: 4px;
      }
      .medidor {
        height: 4px;
        margin: 14px 0 18px;
        background: #e1e5d7;
        border-radius: 4px;
        overflow: hidden;
      }
      .medidor span {
        display: block;
        height: 100%;
        background: #204d36;
        transition: width 0.8s ease;
      }
      ol {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
        gap: 10px;
      }
      li {
        min-width: 0;
        display: grid;
        align-content: start;
        gap: 7px;
        padding: 14px;
        background: #fdfcf7;
        border: 1px solid #daddce;
        border-radius: 7px;
      }
      li.hecho {
        border-color: #adc29b;
        background: #f0f4e9;
      }
      li.actual {
        border: 1px solid #72945d;
        box-shadow: inset 3px 0 #204d36;
      }
      .titulo-parada {
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .numero-parada {
        display: grid;
        place-items: center;
        flex: 0 0 23px;
        height: 23px;
        border-radius: 50%;
        background: #e7ecde;
        color: #526845;
        font-size: 10px;
      }
      .hecho .numero-parada {
        background: #204d36;
        color: white;
      }
      b {
        font-size: 10px;
        color: #63705c;
        font-weight: 500;
      }
      strong {
        font-weight: 600;
        overflow-wrap: anywhere;
      }
      small,
      li > span,
      p {
        font-size: 11px;
        color: #63705c;
        line-height: 1.6;
      }
      p {
        margin: 14px 0 0;
      }
      .estado-tramo {
        color: #204d36;
      }
      details {
        margin-top: 14px;
      }
      summary {
        cursor: pointer;
        color: #204d36;
      }
      .tiempos-reales {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 10px;
        margin-top: 16px;
      }
      .tiempos-reales > div {
        padding: 12px;
        background: #e9eddf;
        border-radius: 5px;
      }
      .tiempos-reales strong {
        display: block;
        margin-top: 5px;
        font-size: 16px;
      }
      @keyframes motor {
        0%,
        100% {
          transform: translateY(0);
        }
        50% {
          transform: translateY(-2px);
        }
      }
      @media (max-width: 550px) {
        .avance {
          padding: 14px;
        }
        .tiempos-reales {
          grid-template-columns: 1fr;
        }
        .carretera {
          padding-left: 22px;
          padding-right: 22px;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        .camion,
        .medidor span,
        .via-completa {
          transition: none;
        }
        .camion.en-marcha svg {
          animation: none;
        }
      }
    `,
  ],
})
export class AvanceViajeComponent {
  @Input({ required: true }) servicio!: ServicioTransporte;
  @Input() pendientes: MarcacionPendiente[] = [];
  get indiceActivo(): number {
    const i = this.tramos.findIndex((t) => !t.llegadaReal);
    return i < 0 ? this.tramos.length - 1 : i;
  }
  get enCamino(): boolean {
    const t = this.tramos[this.indiceActivo];
    return (
      this.servicio.estado !== 'CANCELADO' &&
      this.servicio.estado !== 'INTERRUMPIDO' &&
      !this.servicio.detenidoPorAveria &&
      !!t?.salidaReal &&
      !t.llegadaReal
    );
  }
  get posicionCamion(): number {
    const ts = this.tramos;
    if (!ts.length) return 0;
    const i = ts.findIndex((t) => !t.llegadaReal);
    if (i < 0) return 100;
    return ((i + (ts[i].salidaReal?.length ? 0.5 : 0)) * 100) / ts.length;
  }
  get averiaActiva() {
    return this.servicio.incidencias?.find(
      (i) => !['RESUELTA', 'RETORNO_CONFIRMADO'].includes(i.estado),
    );
  }
  get estadoActual(): string {
    const ts = this.tramos;
    if (this.averiaActiva || this.servicio.detenidoPorAveria)
      return (
        (
          {
            REPORTADA: 'Avería reportada · esperando atención',
            TECNICO_EN_CAMINO: 'Detenido por avería · técnico asignado',
            LISTA_CONTINUAR: 'Reparación registrada · pendiente de continuar',
            REMOLQUE_ASIGNADO: 'Detenido por avería · remolque asignado',
          } as Record<string, string>
        )[this.averiaActiva?.estado ?? ''] || 'Detenido por avería'
      );
    if (this.servicio.estado === 'INTERRUMPIDO')
      return 'Interrumpido por avería · retorno a planta';
    if (this.servicio.estado === 'CANCELADO') return 'Viaje cancelado';
    if (ts.length && ts.every((t) => t.llegadaReal)) return 'Viaje finalizado · regreso confirmado';
    if (this.enCamino) return 'En camino a ' + ts[this.indiceActivo].destino;
    if (!ts[0]?.salidaReal) return 'Pendiente de salida';
    return 'En parada · ' + (ts[this.indiceActivo - 1]?.destino ?? ts[0].origen);
  }
  actividad(a: string): string {
    return a === 'RECOJO'
      ? 'Recoger carga'
      : a === 'ENTREGA'
        ? 'Entregar carga'
        : a === 'RETORNO'
          ? 'Retorno'
          : a === 'AUXILIO'
            ? 'Atender remolque'
            : 'Parada de paso';
  }
  get odometroSalida(): number | null {
    return this.tramos[0]?.odometroSalidaKm ?? null;
  }
  get odometroFinal(): number | null {
    return this.tramos.at(-1)?.odometroLlegadaKm ?? null;
  }
  get distanciaReal(): number | null {
    const d = evaluarDistancia({ ...this.servicio, tramos: this.tramos });
    return d.fuente === 'ODOMETRO' ? Number(d.km!.toFixed(2)) : null;
  }
  get diferencia(): number | null {
    return this.distanciaReal != null && this.servicio.distanciaEstimadaKm != null
      ? Number((this.distanciaReal - this.servicio.distanciaEstimadaKm).toFixed(2))
      : null;
  }
  get tiemposReales(): { conduccion: number; paradas: number; completo: boolean } | null {
    const ts = this.tramos;
    if (!ts.some((t) => t.salidaReal)) return null;
    const minutos = (a: string | null, b: string | null) => {
      if (!a || !b) return 0;
      const n = (new Date(b).getTime() - new Date(a).getTime()) / 60000;
      return Number.isFinite(n) && n >= 0 ? Math.max(0, n - minutosAveria(this.servicio, a, b)) : 0;
    };
    return {
      conduccion: Math.round(ts.reduce((n, t) => n + minutos(t.salidaReal, t.llegadaReal), 0)),
      paradas: Math.round(
        ts.slice(1).reduce((n, t, i) => n + minutos(ts[i].llegadaReal, t.salidaReal), 0),
      ),
      completo: ts.every((t) => !!t.llegadaReal),
    };
  }
  permanencia(i: number): number | null {
    const ts = this.tramos,
      a = ts[i]?.llegadaReal,
      b = ts[i + 1]?.salidaReal;
    if (!a || !b) return null;
    const n = (new Date(b).getTime() - new Date(a).getTime()) / 60000;
    return Number.isFinite(n) && n >= 0 ? Math.round(n) : null;
  }
  get duracionTotalReal(): number | null {
    const ts = this.tramos,
      a = ts[0]?.salidaReal,
      b = ts.at(-1)?.llegadaReal;
    if (!a || !b || !ts.every((t) => t.llegadaReal)) return null;
    const n = (new Date(b).getTime() - new Date(a).getTime()) / 60000;
    return Number.isFinite(n) && n >= 0 ? Math.round(n) : null;
  }
  mostrarMapa = false;
  get hayRecorrido() {
    return (this.servicio.tramos ?? []).some(
      (t) => t.fuenteDistancia === 'CARRETERA' && !!t.recorridoJson,
    );
  }
  get recorridos(): [number, number][][] {
    return (this.servicio.tramos ?? []).flatMap((t) => {
      if (t.fuenteDistancia !== 'CARRETERA' || !t.recorridoJson) return [];
      try {
        const puntos: unknown = JSON.parse(t.recorridoJson);
        return Array.isArray(puntos) &&
          puntos.length > 1 &&
          puntos.every(
            (p) =>
              Array.isArray(p) &&
              p.length === 2 &&
              p.every(Number.isFinite) &&
              Math.abs(p[0]) <= 90 &&
              Math.abs(p[1]) <= 180,
          )
          ? [puntos as [number, number][]]
          : [];
      } catch {
        return [];
      }
    });
  }
  get puntosMapa(): PuntoMapa[] {
    return (this.servicio.tramos ?? []).flatMap((t) =>
      [t.origenUbicacion, t.destinoUbicacion].flatMap((u) =>
        u?.latitud != null && u.longitud != null
          ? [{ nombre: u.nombre, latitud: u.latitud, longitud: u.longitud }]
          : [],
      ),
    );
  }

  get tramos(): TramoServicio[] {
    return tramosConPendientes(this.servicio, this.pendientes);
  }
  get completados(): number {
    return this.tramos.filter((t) => t.llegadaReal).length;
  }
  get porcentaje(): number {
    return this.tramos.length ? Math.round((this.completados * 100) / this.tramos.length) : 0;
  }
  get sinSincronizar(): boolean {
    return this.pendientes.some((e) => e.servicioId === this.servicio.id);
  }
  get ultimaFecha(): string | null {
    return (
      this.tramos
        .flatMap((t) => [t.salidaReal, t.llegadaReal])
        .filter((f): f is string => !!f)
        .sort()
        .at(-1) ?? null
    );
  }
  fecha(valor: string | null): string {
    return valor ? valor.replace('T', ' ').slice(0, 16) : '';
  }
}
