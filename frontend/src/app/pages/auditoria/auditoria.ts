import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { HttpErrorResponse } from '@angular/common/http';

import { Router } from '@angular/router';

import {
  Auditoria as RegistroAuditoria,
  AuditoriaService,
  PaginaAuditoria,
  ResultadoAuditoria,
} from '../../services/auditoria.service';

import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-auditoria',
  imports: [ReactiveFormsModule],
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.scss',
})
export class AuditoriaPage implements OnInit {
  private readonly fb = inject(FormBuilder);

  private readonly router = inject(Router);

  private readonly auditoriaApi = inject(AuditoriaService);

  private readonly sesionService = inject(SesionService);

  readonly pagina = signal<PaginaAuditoria | null>(null);

  readonly cargando = signal(true);

  readonly mensajeError = signal('');

  readonly registroSeleccionado = signal<RegistroAuditoria | null>(null);

  readonly mostrarDetalle = signal(false);

  readonly filtros = this.fb.nonNullable.group({
    texto: '',
    usuarioCorreo: '',
    accion: '',

    resultado: this.fb.nonNullable.control<ResultadoAuditoria | ''>(''),

    fechaDesde: '',
    fechaHasta: '',
  });

  readonly registros = computed(() => this.pagina()?.content ?? []);

  readonly totalRegistros = computed(() => this.pagina()?.totalElements ?? 0);

  readonly paginaActual = computed(() => this.pagina()?.number ?? 0);

  readonly totalPaginas = computed(() => this.pagina()?.totalPages ?? 0);

  readonly puedeRetroceder = computed(() => !(this.pagina()?.first ?? true));

  readonly puedeAvanzar = computed(() => !(this.pagina()?.last ?? true));

  ngOnInit(): void {
    if (!this.validarAcceso()) {
      return;
    }

    this.cargarRegistros(0);
  }

  aplicarFiltros(): void {
    this.cargarRegistros(0);
  }

  limpiarFiltros(): void {
    this.filtros.reset({
      texto: '',
      usuarioCorreo: '',
      accion: '',
      resultado: '',
      fechaDesde: '',
      fechaHasta: '',
    });

    this.cargarRegistros(0);
  }

  cargarRegistros(numeroPagina: number): void {
    if (numeroPagina < 0) {
      return;
    }

    this.cargando.set(true);
    this.mensajeError.set('');

    const valores = this.filtros.getRawValue();

    this.auditoriaApi
      .consultar({
        texto: valores.texto.trim(),

        usuarioCorreo: valores.usuarioCorreo.trim(),

        accion: valores.accion,

        modulo: 'AUTENTICACION',

        resultado: valores.resultado,

        fechaDesde: valores.fechaDesde,

        fechaHasta: valores.fechaHasta,

        pagina: numeroPagina,

        tamanio: 20,
      })
      .subscribe({
        next: (resultado) => {
          this.pagina.set(resultado);
          this.cargando.set(false);
        },

        error: (error) => {
          this.cargando.set(false);

          this.manejarError(error, 'No fue posible consultar ' + 'la auditoría.');
        },
      });
  }

  paginaAnterior(): void {
    if (!this.puedeRetroceder()) {
      return;
    }

    this.cargarRegistros(this.paginaActual() - 1);
  }

  paginaSiguiente(): void {
    if (!this.puedeAvanzar()) {
      return;
    }

    this.cargarRegistros(this.paginaActual() + 1);
  }

  abrirDetalle(registro: RegistroAuditoria): void {
    this.registroSeleccionado.set(registro);

    this.mostrarDetalle.set(true);
  }

  cerrarDetalle(): void {
    this.mostrarDetalle.set(false);

    this.registroSeleccionado.set(null);
  }

  formatearFecha(fecha: string): string {
    if (!fecha) {
      return '—';
    }

    const valor = new Date(fecha);

    if (Number.isNaN(valor.getTime())) {
      return fecha.replace('T', ' ').slice(0, 19);
    }

    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      timeStyle: 'medium',
    }).format(valor);
  }

  formatearJson(valor: string | null): string {
    if (!valor) {
      return 'Sin información';
    }

    try {
      return JSON.stringify(JSON.parse(valor), null, 2);
    } catch {
      return valor;
    }
  }

  nombreModulo(modulo: string): string {
    const nombres: Record<string, string> = {
      PERSONAL_ADMINISTRATIVO: 'Personal administrativo',

      VEHICULOS: 'Vehículos',

      CONDUCTORES: 'Conductores',

      RUTAS: 'Rutas',

      SOLICITUDES: 'Solicitudes',

      SERVICIOS: 'Servicios',

      MANTENIMIENTO: 'Mantenimiento',

      EVIDENCIAS: 'Evidencias',

      ENCUESTAS: 'Encuestas',

      AUTENTICACION: 'Autenticación',
    };

    return nombres[modulo] || modulo.replaceAll('_', ' ');
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irPersonalAdministrativo(): void {
    this.router.navigate(['/personal-administrativo']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  /*
   * Devuelve false cuando el usuario no debe
   * permanecer en esta pantalla.
   */
  private validarAcceso(): boolean {
    const rol = sessionStorage.getItem('rol');

    if (rol !== 'MASTER') {
      this.router.navigate(['/dashboard'], {
        replaceUrl: true,
      });

      return false;
    }

    return true;
  }

  private manejarError(error: HttpErrorResponse, mensajePredeterminado: string): void {
    /*
     * Un 401 significa que el token ya no es válido.
     * No se llama al endpoint de logout porque el
     * backend ya rechazó la autenticación.
     */
    if (error.status === 401) {
      this.sesionService.cerrarSesionLocal();

      return;
    }

    /*
     * Un 403 significa que el usuario inició sesión,
     * pero no posee permisos administrativos.
     */
    if (error.status === 403) {
      this.router.navigate(['/dashboard'], {
        replaceUrl: true,
      });

      return;
    }

    this.mensajeError.set(error.error?.mensaje || error.error?.message || mensajePredeterminado);
  }
}
