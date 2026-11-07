import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { DatePipe } from '@angular/common';

import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin, map } from 'rxjs';

import {
  ServicioTransporte,
  ServicioTransporteService,
} from '../../services/servicio-transporte.service';

import { EstadoVehiculo, Vehiculo, VehiculoService } from '../../services/vehiculo.service';

import { SesionService } from '../../services/sesion.service';
import {
  MantenimientoService,
  PrioridadMantenimiento,
  TipoFalla,
} from '../../services/mantenimiento.service';
import { FotoVehiculoComponent } from '../../components/foto-vehiculo/foto-vehiculo';

type FiltroFlota = 'TODOS' | 'PROGRAMADO' | EstadoVehiculo;

interface UnidadMonitoreada {
  vehiculo: Vehiculo;
  servicio: ServicioTransporte | null;
}

@Component({
  selector: 'app-flota',
  imports: [DatePipe, FotoVehiculoComponent],
  templateUrl: './flota.html',
  styleUrl: './flota.scss',
})
export class Flota implements OnInit {
  private readonly dialogos = inyectarDialogos(DialogosService);
  private readonly rutaActual = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly vehiculoService = inject(VehiculoService);

  private readonly servicioService = inject(ServicioTransporteService);

  private readonly sesionService = inject(SesionService);

  private readonly mantenimientoService = inject(MantenimientoService);

  readonly vehiculos = signal<Vehiculo[]>([]);

  readonly servicios = signal<ServicioTransporte[]>([]);

  readonly cargando = signal(true);

  readonly procesandoId = signal<number | null>(null);

  readonly filtro = signal<FiltroFlota>('TODOS');

  readonly mensajeError = signal('');

  readonly mensajeExito = signal('');

  readonly mantenimientoPendiente = signal<Vehiculo | null>(null);
  readonly prioridadMantenimiento = signal<PrioridadMantenimiento>('BAJA');
  readonly tipoFallaMantenimiento = signal<TipoFalla>('OTRO');
  readonly tituloMantenimiento = signal('');
  readonly descripcionMantenimiento = signal('');
  readonly kilometrajeMantenimiento = signal<number | null>(null);

  actualizarKilometrajeMantenimiento(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.kilometrajeMantenimiento.set(input.value === '' ? null : input.valueAsNumber);
  }
  private confirmandoMantenimiento = false;

  readonly totalUnidades = computed(() => this.vehiculos().length);

  readonly totalDisponibles = computed(
    () =>
      this.vehiculos().filter(
        (vehiculo) =>
          vehiculo.estado === 'DISPONIBLE' &&
          !this.servicios().some(
            (servicio) =>
              servicio.vehiculo.id === vehiculo.id &&
              (servicio.estado === 'PROGRAMADO' || servicio.estado === 'EN_CURSO'),
          ),
      ).length,
  );

  readonly totalProgramados = computed(
    () =>
      new Set(
        this.servicios()
          .filter((servicio) => servicio.estado === 'PROGRAMADO')
          .map((servicio) => servicio.vehiculo.id),
      ).size,
  );

  readonly totalEnServicio = computed(() => this.contarEstado('EN_SERVICIO'));

  readonly totalMantenimiento = computed(() => this.contarEstado('MANTENIMIENTO'));

  readonly totalSuspendidos = computed(() => this.contarEstado('SUSPENDIDO'));
  readonly totalBajas = computed(() => this.contarEstado('BAJA_DEFINITIVA'));
  readonly totalInactivos = computed(() => this.contarEstado('INACTIVO'));

  readonly unidades = computed<UnidadMonitoreada[]>(() => {
    const serviciosActivos = this.servicios().filter(
      (servicio) => servicio.estado === 'PROGRAMADO' || servicio.estado === 'EN_CURSO',
    );

    return this.vehiculos()
      .map((vehiculo) => {
        const serviciosUnidad = serviciosActivos.filter(
          (servicio) => servicio.vehiculo.id === vehiculo.id,
        );

        const servicioEnCurso = serviciosUnidad.find((servicio) => servicio.estado === 'EN_CURSO');

        const servicioProgramado = serviciosUnidad.find(
          (servicio) => servicio.estado === 'PROGRAMADO',
        );

        return {
          vehiculo,
          servicio: servicioEnCurso ?? servicioProgramado ?? null,
        };
      })
      .filter((unidad) => {
        if (this.filtro() === 'TODOS') {
          return true;
        }

        if (this.filtro() === 'PROGRAMADO') {
          return unidad.servicio?.estado === 'PROGRAMADO';
        }

        if (this.filtro() === 'DISPONIBLE') {
          return unidad.vehiculo.estado === 'DISPONIBLE' && unidad.servicio === null;
        }

        return unidad.vehiculo.estado === this.filtro();
      })
      .sort(
        (primera, segunda) =>
          this.prioridadEstado(primera.vehiculo.estado) -
          this.prioridadEstado(segunda.vehiculo.estado),
      );
  });

  ngOnInit(): void {
    this.cargarMonitoreo();
  }

  cargarMonitoreo(): void {
    this.cargando.set(true);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    forkJoin({
      vehiculos: this.vehiculoService.listar(),

      servicios: this.servicioService.listar(),
    }).subscribe({
      next: ({ vehiculos, servicios }) => {
        this.vehiculos.set(vehiculos);
        this.servicios.set(servicios);
        this.cargando.set(false);
        const unidadId = Number(this.rutaActual.snapshot.queryParamMap.get('unidad'));
        if (unidadId > 0) {
          this.filtro.set('TODOS');
          setTimeout(() =>
            document.getElementById('unidad-' + unidadId)?.scrollIntoView({ block: 'center' }),
          );
        }
      },

      error: (error) => {
        this.cargando.set(false);

        if (error.status === 401 || error.status === 403) {
          this.sesionService.cerrarSesionLocal();

          return;
        }

        this.mensajeError.set('No fue posible cargar ' + 'el monitoreo de flota.');
      },
    });
  }

  seleccionarFiltro(filtro: FiltroFlota): void {
    this.filtro.set(filtro);
  }

  async cambiarEstado(vehiculo: Vehiculo, nuevoEstado: EstadoVehiculo): Promise<void> {
    if (this.procesandoId() !== null) {
      return;
    }

    if (
      nuevoEstado === 'INACTIVO' ||
      (nuevoEstado === 'DISPONIBLE' && ['INACTIVO', 'SUSPENDIDO'].includes(vehiculo.estado))
    ) {
      void this.router.navigate(['/vehiculos'], {
        queryParams: { unidad: vehiculo.id, accion: 'BAJA' },
      });
      return;
    }
    if (nuevoEstado === 'MANTENIMIENTO' && !this.confirmandoMantenimiento) {
      this.mensajeError.set('');
      this.prioridadMantenimiento.set('BAJA');
      this.tipoFallaMantenimiento.set('OTRO');
      this.tituloMantenimiento.set('');
      this.descripcionMantenimiento.set('');
      this.kilometrajeMantenimiento.set(null);
      this.mantenimientoPendiente.set(vehiculo);
      return;
    }

    const accion =
      nuevoEstado === 'MANTENIMIENTO'
        ? 'enviar a mantenimiento a la unidad seleccionada'
        : 'devolver a disponible';

    const confirmado = this.confirmandoMantenimiento
      ? true
      : await this.dialogos.confirmar(`¿Deseas ${accion} la unidad ` + `${vehiculo.placa}?`);

    if (!confirmado) {
      return;
    }

    this.procesandoId.set(vehiculo.id);

    this.mensajeError.set('');
    this.mensajeExito.set('');

    const actualizacion =
      nuevoEstado === 'MANTENIMIENTO'
        ? this.mantenimientoService
            .registrar({
              vehiculoId: vehiculo.id,
              tipoFalla: this.tipoFallaMantenimiento(),
              prioridad: this.prioridadMantenimiento(),
              titulo: this.tituloMantenimiento().trim() || 'Falla reportada',
              descripcion:
                this.descripcionMantenimiento().trim() ||
                'Unidad enviada a mantenimiento desde el monitoreo de flota.',
              kilometrajeReporte: this.kilometrajeMantenimiento(),
            })
            .pipe(
              map(() => ({
                ...vehiculo,
                estado: nuevoEstado,
              })),
            )
        : this.vehiculoService.cambiarEstado(vehiculo.id, nuevoEstado);

    actualizacion.subscribe({
      next: (actualizado) => {
        if (nuevoEstado === 'MANTENIMIENTO') {
          this.mantenimientoPendiente.set(null);
        }
        this.vehiculos.update((lista) =>
          lista.map((item) => (item.id === actualizado.id ? actualizado : item)),
        );

        this.procesandoId.set(null);

        this.mensajeExito.set(`La unidad ${actualizado.placa} ` + 'se actualizó correctamente.');
      },

      error: (error) => {
        this.procesandoId.set(null);

        if (error.status === 401) {
          this.sesionService.cerrarSesionLocal();

          return;
        }

        this.mensajeError.set(
          error.error?.mensaje ??
            error.error?.message ??
            (error.status === 403
              ? 'La unidad tiene un servicio ' + 'activo y no puede cambiar ' + 'de estado.'
              : 'No fue posible cambiar ' + 'el estado de la unidad.'),
        );
      },
    });
  }

  cancelarEnvioMantenimiento(): void {
    if (this.procesandoId() !== null) return;
    this.mantenimientoPendiente.set(null);
  }

  confirmarEnvioMantenimiento(): void {
    const vehiculo = this.mantenimientoPendiente();
    if (!vehiculo || this.procesandoId() !== null) return;
    const km = this.kilometrajeMantenimiento();
    if (km !== null && (!Number.isInteger(km) || km < 0 || km > 2147483647)) {
      this.mensajeError.set(
        'Ingresa un kilometraje entero, igual o mayor que cero y dentro del rango permitido.',
      );
      return;
    }
    this.confirmandoMantenimiento = true;
    void this.cambiarEstado(vehiculo, 'MANTENIMIENTO').finally(
      () => (this.confirmandoMantenimiento = false),
    );
  }

  etiquetaEstado(estado: EstadoVehiculo): string {
    switch (estado) {
      case 'DISPONIBLE':
        return 'Disponible';

      case 'EN_SERVICIO':
        return 'En operación';

      case 'MANTENIMIENTO':
        return 'En mantenimiento';

      case 'INACTIVO':
        return 'Pendiente de clasificar';
      case 'SUSPENDIDO':
        return 'Registro anterior de prueba';
      case 'BAJA_DEFINITIVA':
        return 'Fuera de servicio — baja definitiva';
    }
  }

  etiquetaProducto(producto: string): string {
    const etiquetas: Record<string, string> = {
      RFF: 'Racimo de Fruto Fresco',
      FS: 'Fruto Suelto',
      ESCOBAJO: 'Escobajo',
      FIBRA: 'Fibra',

      TORTA_CLARIFICACION: 'Torta de clarificación',

      LODO_BIOGAS: 'Lodo de biogás',

      CENIZA: 'Ceniza',

      RESIDUOS_VARIOS: 'Residuos varios',
    };

    return etiquetas[producto] ?? producto.replaceAll('_', ' ');
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irVehiculos(): void {
    this.router.navigate(['/vehiculos']);
  }

  irServicios(): void {
    this.router.navigate(['/servicios']);
  }

  verGestionUnidad(id: number): void {
    void this.router.navigate(['/vehiculos'], { queryParams: { unidad: id } });
  }
  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private contarEstado(estado: EstadoVehiculo): number {
    return this.vehiculos().filter((vehiculo) => vehiculo.estado === estado).length;
  }

  private prioridadEstado(estado: EstadoVehiculo): number {
    const prioridades: Record<EstadoVehiculo, number> = {
      EN_SERVICIO: 1,
      MANTENIMIENTO: 2,
      DISPONIBLE: 3,
      INACTIVO: 4,
      SUSPENDIDO: 5,
      BAJA_DEFINITIVA: 6,
    };

    return prioridades[estado];
  }
}
