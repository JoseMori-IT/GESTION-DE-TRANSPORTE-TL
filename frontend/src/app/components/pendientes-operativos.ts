import { Component, DestroyRef, ElementRef, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Mantenimiento } from '../services/mantenimiento.service';
import { SolicitudTransporte } from '../services/solicitud-transporte';
import { ServicioTransporte } from '../services/servicio-transporte.service';
export interface PendienteOperativo {
  clave: string;
  titulo: string;
  detalle: string;
  accion: string;
  ruta: string;
  id: number;
  prioridad: number;
}
export function pendientesOperativos(
  m: Mantenimiento[],
  s: SolicitudTransporte[],
  v: ServicioTransporte[],
): PendienteOperativo[] {
  const lista: PendienteOperativo[] = [];
  const vinculados = new Set<number>();
  for (const viaje of v.filter((x) => x.estado === 'EN_CURSO')) {
    const incidencia = viaje.incidencias?.find(
      (i) => !['RESUELTA', 'RETORNO_CONFIRMADO'].includes(i.estado),
    );
    if (!incidencia) continue;
    if (incidencia.mantenimientoId) vinculados.add(incidencia.mantenimientoId);
    lista.push({
      clave: 'viaje-' + viaje.id,
      titulo: 'Avería en ruta',
      detalle: `${viaje.codigo} · ${viaje.vehiculo.placa}`,
      accion: 'Atender avería',
      ruta: '/servicios',
      id: viaje.id,
      prioridad: 0,
    });
  }
  const pasos: Record<string, string> = {
    REPORTADO: 'Mantenimiento por atender',
    EN_EVALUACION: 'Evaluación de mantenimiento',
    EN_MANTENIMIENTO: 'Mantenimiento en proceso',
    REPARADO: 'Mantenimiento pendiente de cierre',
  };
  for (const x of m) {
    if (!pasos[x.estado] || vinculados.has(x.id)) continue;
    lista.push({
      clave: 'man-' + x.id,
      titulo: pasos[x.estado],
      detalle: `${x.vehiculoPlaca} · ${x.titulo}`,
      accion: 'Revisar mantenimiento',
      ruta: '/mantenimiento',
      id: x.id,
      prioridad: x.prioridad === 'CRITICA' ? 0 : x.estado === 'REPORTADO' ? 1 : 3,
    });
  }
  const estados: Record<string, string> = {
    SOLICITADA: 'Solicitud por revisar',
    EN_REVISION: 'Solicitud pendiente de aprobación',
    APROBADA: 'Solicitud lista para programar',
  };
  for (const x of s) {
    if (!estados[x.estado]) continue;
    lista.push({
      clave: 'sol-' + x.id,
      titulo: estados[x.estado],
      detalle: `${x.codigo} · ${x.solicitanteNombre}`,
      accion:
        x.estado === 'APROBADA'
          ? 'Programar servicio'
          : x.estado === 'EN_REVISION'
            ? 'Revisar aprobación'
            : 'Revisar solicitud',
      ruta: '/solicitudes',
      id: x.id,
      prioridad: 2,
    });
  }
  return lista.sort(
    (a, b) => a.prioridad - b.prioridad || b.id - a.id || a.clave.localeCompare(b.clave),
  );
}
@Component({
  selector: 'app-pendientes-operativos',
  template: ` <div class="contenedor">
    <button
      class="abrir"
      type="button"
      [attr.aria-expanded]="abierto()"
      aria-controls="pendientes-operativos"
      (click)="alternar()"
    >
      Pendientes <span>{{ error() ? '—' : items().length }}</span>
    </button>
    @if (abierto()) {
      <section id="pendientes-operativos" aria-label="Pendientes de la operación">
        <header>
          <div><h2>Pendientes de atención</h2></div>
          <button
            type="button"
            class="cerrar"
            aria-label="Cerrar pendientes"
            (click)="abierto.set(false)"
          >
            ×
          </button>
        </header>
        <div class="actualizacion">
          <span role="status">{{
            cargando() ? 'Consultando…' : actualizado() ? 'Actualizado ' + actualizado() : ''
          }}</span
          ><button type="button" [disabled]="cargando()" (click)="cargar()">Actualizar</button>
        </div>
        @if (error()) {
          <p class="error" role="alert">
            No se pudieron consultar los pendientes. Pulsa Actualizar para reintentar.
          </p>
        } @else if (!items().length && !cargando()) {
          <p class="vacio">No hay procesos pendientes de atención.</p>
        }
        <div class="lista">
          @for (item of items(); track item.clave) {
            <button type="button" class="aviso" (click)="abrir(item)">
              <span class="titulo" [class.urgente]="item.prioridad === 0">{{ item.titulo }}</span
              ><span class="detalle">{{ item.detalle }}</span
              ><span class="destino">{{ item.accion }} →</span>
            </button>
          }
        </div>
      </section>
    }
  </div>`,
  styles: [
    `
      :host {
        display: block;
        position: relative;
        z-index: 30;
      }
      .contenedor {
        position: relative;
      }
      button {
        font: inherit;
        cursor: pointer;
        min-height: 44px;
      }
      .abrir {
        display: flex;
        align-items: center;
        gap: 14px;
        border: 1px solid #dde0e8;
        border-radius: 12px;
        background: #fff;
        padding: 10px 16px;
        font-size: 14px;
        font-weight: 600;
        color: #1d1d1f;
      }
      .abrir span {
        border-radius: 20px;
        padding: 3px 9px;
        background: #eaf3ff;
        color: #006ad6;
        font-size: 12px;
      }
      section {
        position: absolute;
        right: 0;
        top: calc(100% + 10px);
        width: min(440px, calc(100vw - 48px));
        background: white;
        border: 1px solid #e0e2e9;
        border-radius: 18px;
        box-shadow: 0 18px 60px #18243c26;
        overflow: hidden;
        animation: entrada 0.18s ease-out;
      }
      header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        padding: 18px;
        border-bottom: 1px solid #eceef3;
      }
      h2 {
        margin: 0 0 5px;
        font-size: 17px;
        color: #1d1d1f;
      }
      small {
        font-size: 12px;
        color: #626875;
      }
      .cerrar {
        width: 36px;
        min-height: 36px;
        border: 0;
        border-radius: 50%;
        background: #f5f5f7;
        color: #555;
        font-size: 23px;
      }
      .actualizacion {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        padding: 8px 18px;
        color: #626875;
        font-size: 12px;
      }
      .actualizacion button {
        border: 0;
        background: transparent;
        color: #006ad6;
        font-size: 12px;
      }
      .lista {
        max-height: min(450px, 60dvh);
        overflow-y: auto;
        overscroll-behavior: contain;
      }
      .aviso {
        display: grid;
        gap: 6px;
        text-align: left;
        width: 100%;
        border: 0;
        border-top: 1px solid #eceef3;
        background: white;
        padding: 16px 18px;
      }
      .aviso:hover {
        background: #f6f8fc;
      }
      .titulo {
        font-size: 14px;
        font-weight: 600;
        color: #1d1d1f;
      }
      .urgente {
        color: #b04d00;
      }
      .detalle {
        font-size: 12px;
        color: #626875;
        overflow-wrap: anywhere;
      }
      .destino {
        font-size: 12px;
        color: #006ad6;
        font-weight: 500;
      }
      .vacio,
      .error {
        padding: 20px;
        font-size: 13px;
        line-height: 1.6;
        color: #626875;
        margin: 0;
      }
      .error {
        color: #ad2d3d;
      }
      button:focus-visible {
        outline: 3px solid #007aff;
        outline-offset: -3px;
      }
      button:disabled {
        opacity: 0.5;
        cursor: wait;
      }
      @keyframes entrada {
        from {
          opacity: 0;
          transform: translateY(-5px);
        }
        to {
          opacity: 1;
          transform: none;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        section {
          animation: none;
        }
      }
      @media (max-width: 600px) {
        section {
          right: auto;
          left: 0;
        }
      }
    `,
  ],
  host: { '(document:click)': 'fuera($event)', '(document:keydown.escape)': 'cerrarConEscape()' },
})
export class PendientesOperativosComponent implements OnInit {
  readonly abierto = signal(false);
  readonly items = signal<PendienteOperativo[]>([]);
  readonly cargando = signal(false);
  readonly error = signal(false);
  readonly actualizado = signal('');
  private http = inject(HttpClient);
  private router = inject(Router);
  private host: ElementRef<HTMLElement> = inject(ElementRef);
  private destroy = inject(DestroyRef);
  ngOnInit() {
    this.cargar();
    const t = setInterval(() => {
      if (!document.hidden) this.cargar();
    }, 30000);
    this.destroy.onDestroy(() => clearInterval(t));
  }
  alternar() {
    this.abierto.update((v) => !v);
    if (this.abierto()) this.cargar();
  }
  cargar() {
    if (this.cargando()) return;
    this.cargando.set(true);
    forkJoin({
      m: this.http.get<Mantenimiento[]>('/api/mantenimientos'),
      s: this.http.get<SolicitudTransporte[]>('/api/solicitudes'),
      v: this.http.get<ServicioTransporte[]>('/api/servicios'),
    })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: ({ m, s, v }) => {
          this.items.set(pendientesOperativos(m, s, v));
          this.error.set(false);
          this.actualizado.set(
            new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }),
          );
          this.cargando.set(false);
        },
        error: () => {
          this.items.set([]);
          this.error.set(true);
          this.cargando.set(false);
        },
      });
  }
  abrir(p: PendienteOperativo) {
    this.abierto.set(false);
    void this.router.navigate([p.ruta], { queryParams: { atender: p.id } });
  }
  fuera(e: Event) {
    if (!this.host.nativeElement.contains(e.target as Node)) this.abierto.set(false);
  }
  cerrarConEscape() {
    if (this.abierto()) {
      this.abierto.set(false);
      this.host.nativeElement.querySelector<HTMLButtonElement>('.abrir')?.focus();
    }
  }
}
