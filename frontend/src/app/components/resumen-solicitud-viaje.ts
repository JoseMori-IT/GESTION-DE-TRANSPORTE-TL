import { ServicioTransporte } from '../services/servicio-transporte.service';
import { Component, Input } from '@angular/core';
import { SolicitudTransporte } from '../services/solicitud-transporte';
@Component({
  selector: 'app-resumen-solicitud-viaje',
  template: `
    @if (solicitud.planViaje; as plan) {
      <section class="resumen" aria-label="Recorrido de la solicitud">
        <header>
          <h3>{{ viaje ? 'Plan y ejecución del viaje' : 'Plan del viaje' }}</h3>
          <span class="etiqueta">Ida y vuelta</span>
        </header>
        <p class="puntos">{{ solicitud.resumenRecorrido }}</p>
        <div class="indicadores">
          <div>
            <small>Distancia estimada</small
            ><strong>{{ solicitud.distanciaEstimadaKm ?? '—' }} <span>km</span></strong>
          </div>
          <div>
            <small>Conducción estimada (ida y vuelta)</small
            ><strong>{{ tramos.length ? totalConduccion : '—' }} <span>min</span></strong>
          </div>
          <div>
            <small>Trabajo y espera {{ viaje ? '(registrado)' : '' }}</small>
            @if (totalPermanencia != null) {
              <strong>{{ totalPermanencia }} <span>min</span></strong
              ><small>{{
                permanenciasCompletas ? 'Todas las paradas' : 'Parcial: intervalos cerrados'
              }}</small>
            } @else {
              <strong class="sin-tiempo">{{ sinDatos }}</strong>
            }
          </div>
        </div>
        <h4>Secuencia de traslado y tareas</h4>
        <ol class="secuencia">
          @for (t of tramos; track $index; let i = $index) {
            <li class="traslado">
              <span class="numero">{{ i + 1 }}</span>
              <div>
                <small>{{
                  i === plan.paradas.length ? 'Traslado de regreso' : 'Traslado hacia la parada'
                }}</small
                ><strong>{{ t.nombre }}</strong>
              </div>
              <b
                >{{ t.minutos }} min<small>Estimado</small>
                @if (trasladoReal(i) != null) {
                  <small>{{ trasladoReal(i) }} min registrado</small>
                }
              </b>
            </li>
            @if (i < plan.paradas.length) {
              <li class="tarea">
                <span class="numero" aria-hidden="true">•</span>
                <div>
                  <small>Trabajo en parada {{ i + 1 }}</small
                  ><strong
                    >{{ actividad(plan.paradas[i].actividad) }} ·
                    {{ plan.paradas[i].producto }}</strong
                  >
                  @if (plan.paradas[i].indicaciones) {
                    <p>{{ plan.paradas[i].indicaciones }}</p>
                  }
                </div>
                @if (permanencia(i) != null) {
                  <b>{{ permanencia(i) }} min<small>Trabajo y espera registrados</small></b>
                } @else {
                  <span class="pendiente">{{
                    viaje?.estado === 'COMPLETADO'
                      ? 'Sin marcaciones completas'
                      : viaje
                        ? 'Pendiente de salida'
                        : 'Duración por registrar'
                  }}</span>
                }
              </li>
            }
          } @empty {
            <li>Sin desglose de tiempos registrado.</li>
          }
        </ol>
        <footer>
          <strong
            >Duración total del viaje:
            {{
              duracionReal != null
                ? duracionReal + ' min registrados'
                : viaje?.estado === 'COMPLETADO'
                  ? 'Sin marcaciones completas'
                  : 'pendiente'
            }}</strong
          >
          <p>
            Incluye traslados, trabajo y esperas. Se obtiene desde la salida inicial hasta el
            regreso final.
          </p>
          <p>
            La permanencia en cada punto se mide desde la llegada hasta la siguiente salida. Incluye
            trabajo y espera; no mide exclusivamente la tarea.
          </p>
        </footer>
      </section>
    } @else {
      <p class="pendiente">Solicitud anterior: falta definir y aprobar el recorrido completo.</p>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        grid-column: 1/-1;
        min-width: 0;
      }
      .resumen {
        padding: 18px;
        background: #f3f4ec;
        border: 1px solid #daddce;
        border-radius: 8px;
        color: #204d36;
        white-space: normal;
        font-size: 12px;
      }
      header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
      }
      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
      }
      .etiqueta {
        padding: 5px 9px;
        background: #e3eada;
        border-radius: 4px;
        font-size: 10px;
      }
      .puntos {
        margin: 14px 0;
        font-weight: 500;
        line-height: 1.8;
        overflow-wrap: anywhere;
      }
      .indicadores {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 10px;
        margin: 16px 0 20px;
      }
      .indicadores > div {
        padding: 13px;
        background: #fdfcf7;
        border: 1px solid #dde1d2;
        border-radius: 6px;
      }
      small {
        display: block;
        font-weight: 400;
        color: #68745f;
        font-size: 10px;
        line-height: 1.5;
      }
      .indicadores strong {
        display: block;
        margin-top: 7px;
        font-size: 21px;
        font-weight: 600;
      }
      .indicadores strong span {
        font-size: 12px;
        font-weight: 400;
      }
      .indicadores .sin-tiempo {
        font-size: 12px;
        margin-top: 11px;
      }
      h4 {
        margin: 0 0 12px;
        font-size: 12px;
        font-weight: 600;
      }
      .secuencia {
        list-style: none;
        padding: 0;
        margin: 0;
        display: grid;
        gap: 8px;
      }
      .secuencia li {
        display: grid;
        grid-template-columns: 26px minmax(0, 1fr) auto;
        gap: 10px;
        align-items: center;
        padding: 12px;
        background: #fdfcf7;
        border: 1px solid #e0e4d7;
        border-radius: 6px;
      }
      .numero {
        display: grid;
        place-items: center;
        width: 24px;
        height: 24px;
        background: #e6edde;
        border-radius: 50%;
        font-size: 10px;
      }
      .secuencia strong {
        display: block;
        font-size: 11px;
        font-weight: 500;
        line-height: 1.6;
        overflow-wrap: anywhere;
      }
      .secuencia b {
        font-size: 12px;
        white-space: nowrap;
        text-align: right;
      }
      .secuencia .tarea {
        margin-left: 18px;
        background: #eef0e5;
        border-color: transparent;
      }
      .tarea p {
        margin: 5px 0;
        font-size: 11px;
      }
      .pendiente {
        font-size: 10px;
        color: #68745f;
      }
      footer {
        margin-top: 16px;
        padding-top: 14px;
        border-top: 1px solid #daddce;
      }
      footer strong {
        font-size: 12px;
      }
      footer p {
        margin: 6px 0 0;
        font-size: 11px;
        line-height: 1.7;
        color: #68745f;
      }
      @media (max-width: 550px) {
        .resumen {
          padding: 14px;
        }
        .indicadores {
          grid-template-columns: 1fr;
        }
        .indicadores > div {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 10px;
        }
        .indicadores strong,
        .indicadores .sin-tiempo {
          margin: 0;
        }
        .secuencia li {
          grid-template-columns: 24px minmax(0, 1fr);
        }
        .secuencia li > b,
        .secuencia li > .pendiente {
          grid-column: 2;
          text-align: left;
        }
        .secuencia .tarea {
          margin-left: 10px;
        }
      }
      .resumen {
        background: #fff;
        border-color: #dfe1e8;
        border-radius: 14px;
        color: #1d1d1f;
        padding: 20px;
        font-size: 14px;
      }
      h3 {
        font-size: 18px;
      }
      .etiqueta {
        background: #eaf2ff;
        color: #0066cc;
        font-size: 12px;
        border-radius: 8px;
      }
      .puntos {
        font-size: 14px;
        line-height: 1.8;
      }
      .indicadores > div {
        background: #f5f5f7;
        border-color: #e3e4ea;
        border-radius: 10px;
        padding: 16px;
      }
      small {
        font-size: 12px;
        color: #626773;
      }
      .indicadores strong {
        font-size: 24px;
      }
      .indicadores .sin-tiempo {
        font-size: 13px;
        line-height: 1.5;
      }
      h4 {
        font-size: 14px;
      }
      .secuencia li {
        background: #fff;
        border-color: #dfe1e8;
        padding: 16px;
        border-radius: 10px;
      }
      .secuencia strong {
        font-size: 14px;
      }
      .secuencia b {
        font-size: 14px;
      }
      .numero {
        background: #eaf2ff;
        color: #0066cc;
        font-size: 12px;
      }
      .secuencia .tarea {
        background: #f5f5f7;
        margin-left: 16px;
        border-color: #e3e4ea;
      }
      .tarea .numero {
        display: none;
      }
      .secuencia .tarea {
        grid-template-columns: minmax(0, 1fr) auto;
      }
      .pendiente,
      .tarea p,
      footer p {
        font-size: 12px;
        color: #626773;
      }
      footer {
        border-color: #dfe1e8;
      }
      footer strong {
        font-size: 14px;
      }
      @media (max-width: 550px) {
        .resumen {
          padding: 16px;
        }
        .secuencia .tarea {
          grid-template-columns: 1fr;
          margin-left: 0;
        }
        .secuencia .tarea > .pendiente,
        .secuencia .tarea > b {
          grid-column: 1;
        }
        .indicadores > div {
          display: block;
        }
        .indicadores strong {
          margin-top: 8px;
        }
      }
    `,
  ],
})
export class ResumenSolicitudViajeComponent {
  @Input() viaje: ServicioTransporte | null = null;
  @Input({ required: true }) solicitud!: SolicitudTransporte;
  private minutos(a: string | null | undefined, b: string | null | undefined): number | null {
    if (!a || !b) return null;
    const n = (new Date(b).getTime() - new Date(a).getTime()) / 60000;
    return Number.isFinite(n) && n >= 0 ? n : null;
  }
  private get confirmados() {
    return [...(this.viaje?.tramos ?? [])].sort((a, b) => a.orden - b.orden);
  }
  permanencia(i: number): number | null {
    const ts = this.confirmados;
    const n = this.minutos(ts[i]?.llegadaReal, ts[i + 1]?.salidaReal);
    return n == null ? null : Math.round(n);
  }
  trasladoReal(i: number): number | null {
    const t = this.confirmados[i],
      n = this.minutos(t?.salidaReal, t?.llegadaReal);
    return n == null ? null : Math.round(n);
  }
  get totalPermanencia(): number | null {
    const ts = this.confirmados;
    const ns = ts
      .slice(0, -1)
      .map((t, i) => this.minutos(t.llegadaReal, ts[i + 1]?.salidaReal))
      .filter((n): n is number => n != null);
    return ns.length ? Math.round(ns.reduce((a, b) => a + b, 0)) : null;
  }
  get permanenciasCompletas(): boolean {
    const n = this.solicitud.planViaje?.paradas.length ?? 0;
    return (
      n > 0 &&
      this.confirmados.length === n + 1 &&
      Array.from({ length: n }, (_, i) => this.permanencia(i)).every((v) => v != null)
    );
  }
  get sinDatos(): string {
    return this.viaje?.estado === 'COMPLETADO'
      ? 'Sin marcaciones completas'
      : this.viaje
        ? 'Pendiente de marcaciones'
        : 'Se registra durante el viaje';
  }
  get duracionReal(): number | null {
    const ts = this.confirmados;
    if (!ts.length || !ts.every((t) => t.llegadaReal)) return null;
    const n = this.minutos(ts[0].salidaReal, ts.at(-1)?.llegadaReal);
    return n == null ? null : Math.round(n);
  }
  get tramos(): { nombre: string; minutos: number }[] {
    const plan = this.solicitud.planViaje;
    if (!plan?.estimaciones || plan.estimaciones.length !== plan.paradas.length + 1) return [];
    const nombres = this.solicitud.resumenRecorrido?.split(' → ') ?? [];
    return plan.estimaciones.map((e, i) => ({
      nombre:
        nombres[i] && nombres[i + 1]
          ? `${nombres[i]} → ${nombres[i + 1]}${i === plan.paradas.length ? ' (regreso)' : ''}`
          : `Tramo ${i + 1}${i === plan.paradas.length ? ' · Regreso al origen' : ''}`,
      minutos: e.duracionMinutos,
    }));
  }
  get totalConduccion(): number {
    return this.tramos.reduce((n, t) => n + t.minutos, 0);
  }
  actividad(a: string) {
    return a === 'RECOJO' ? 'Recoger carga' : a === 'ENTREGA' ? 'Entregar carga' : 'Parada de paso';
  }
}
