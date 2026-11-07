import { ConfiguracionOperativaService } from '../../services/configuracion-operativa.service';
import { ConsumoIndicadores } from './consumo-indicadores';
import { ReporteConsumoImpreso } from './reporte-consumo-impreso';
import { calcularConsumo } from './consumo-calculo';
import { ReporteGeneralImpreso } from './reporte-general-impreso';
import { formatearDuracionMinutos } from '../../services/formato-duracion';
import { Indicadores } from './indicadores';
import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { Component, computed, viewChild, OnInit, signal } from '@angular/core';

import { Router } from '@angular/router';

import {
  ServicioTransporte,
  ServicioTransporteService,
} from '../../services/servicio-transporte.service';

import { SesionService } from '../../services/sesion.service';

type EstadoFiltro =
  'TODOS' | 'PROGRAMADO' | 'EN_CURSO' | 'COMPLETADO' | 'CANCELADO' | 'INTERRUMPIDO';

interface ResumenRuta {
  ruta: string;
  viajesCompletados: number;
  duracionPromedioMinutos: number;
}

interface ResumenProducto {
  producto: string;
  cantidad: number;
}

@Component({
  selector: 'app-reportes',
  imports: [Indicadores, ReporteGeneralImpreso, ConsumoIndicadores, ReporteConsumoImpreso],
  templateUrl: './reportes.html',
  styleUrls: ['./reportes.scss', './reportes-apple.scss'],
})
export class Reportes implements OnInit {
  readonly configuracion = inyectarDialogos(ConfiguracionOperativaService);
  private readonly dialogos = inyectarDialogos(DialogosService);

  readonly indicadores = viewChild(Indicadores);
  readonly vistaReporte = signal<'general' | 'chofer' | 'consumo'>('general');
  irAbastecimientos() {
    void this.router.navigate(['/abastecimientos']);
  }
  cambiarVista(vista: 'general' | 'chofer' | 'consumo') {
    this.vistaReporte.set(vista);
    this.choferSeleccionado.set('TODOS');
    this.estadoSeleccionado.set('TODOS');
    this.rutaSeleccionada.set('TODAS');
  }

  readonly consumo = computed(() =>
    calcularConsumo(
      this.servicios(),
      {
        desde: this.fechaDesde(),
        hasta: this.fechaHasta(),
        ruta: this.rutaSeleccionada(),
        chofer: 'TODOS',
        estado: 'TODOS',
      },
      this.configuracion.referencia(),
    ),
  );

  readonly generadoPor = sessionStorage.getItem('nombres') || 'Usuario del sistema';

  readonly servicios = signal<ServicioTransporte[]>([]);

  readonly cargando = signal(false);

  readonly mensajeError = signal('');

  readonly estadoSeleccionado = signal<EstadoFiltro>('TODOS');

  readonly rutaSeleccionada = signal('TODAS');

  readonly choferSeleccionado = signal('TODOS');

  readonly fechaDesde = signal('');

  readonly fechaHasta = signal('');

  readonly rutasDisponibles = computed(() => {
    const nombres = this.servicios()
      .map(
        (servicio) =>
          servicio.ruta?.nombre || `${servicio.origenDescripcion} → ${servicio.destinoDescripcion}`,
      )
      .filter((nombre, indice, arreglo) => arreglo.indexOf(nombre) === indice)
      .sort();

    return nombres;
  });

  readonly choferesDisponibles = computed(() => {
    const choferes = new Map<number, ServicioTransporte['conductor']>();

    for (const servicio of this.servicios()) {
      choferes.set(servicio.conductor.id, servicio.conductor);
    }

    return Array.from(choferes.values()).sort((primero, segundo) => {
      const nombrePrimero = `${primero.nombres} ` + `${primero.apellidos}`;

      const nombreSegundo = `${segundo.nombres} ` + `${segundo.apellidos}`;

      return nombrePrimero.localeCompare(nombreSegundo);
    });
  });

  readonly choferReporte = computed(() => {
    const seleccionado = this.choferSeleccionado();

    if (seleccionado === 'TODOS') {
      return null;
    }

    return this.choferesDisponibles().find((chofer) => chofer.id === Number(seleccionado)) || null;
  });

  readonly serviciosFiltrados = computed(() => {
    const estado = this.estadoSeleccionado();

    const ruta = this.rutaSeleccionada();

    const chofer = this.choferSeleccionado();

    const desde = this.fechaDesde();

    const hasta = this.fechaHasta();

    return this.servicios().filter((servicio) => {
      if (estado !== 'TODOS' && servicio.estado !== estado) {
        return false;
      }

      if (
        ruta !== 'TODAS' &&
        (servicio.ruta?.nombre ||
          `${servicio.origenDescripcion} → ${servicio.destinoDescripcion}`) !== ruta
      ) {
        return false;
      }

      if (chofer !== 'TODOS' && servicio.conductor.id !== Number(chofer)) {
        return false;
      }

      /*
       * Se agrupa por salida programada para mantener estable el período.
       */
      const fechaServicio = servicio.fechaHoraSalida;

      if (!fechaServicio) {
        return !desde && !hasta;
      }

      const fecha = fechaServicio.slice(0, 10);

      if (desde && fecha < desde) {
        return false;
      }

      if (hasta && fecha > hasta) {
        return false;
      }

      return true;
    });
  });

  readonly totalServicios = computed(() => this.serviciosFiltrados().length);

  readonly totalCompletados = computed(
    () => this.serviciosFiltrados().filter((servicio) => servicio.estado === 'COMPLETADO').length,
  );

  readonly totalCancelados = computed(
    () => this.serviciosFiltrados().filter((servicio) => servicio.estado === 'CANCELADO').length,
  );

  readonly totalEnCurso = computed(
    () => this.serviciosFiltrados().filter((servicio) => servicio.estado === 'EN_CURSO').length,
  );

  readonly totalProgramados = computed(
    () => this.serviciosFiltrados().filter((servicio) => servicio.estado === 'PROGRAMADO').length,
  );

  readonly porcentajeCumplimiento = computed(() => {
    const total = this.totalServicios();

    if (total === 0) {
      return 0;
    }

    return Math.round((this.totalCompletados() / total) * 100);
  });

  readonly duracionPromedio = computed(() => {
    const duraciones = this.serviciosFiltrados()
      .map((servicio) => this.obtenerDuracionMinutos(servicio))
      .filter((duracion): duracion is number => duracion !== null);

    if (duraciones.length === 0) {
      return 0;
    }

    const suma = duraciones.reduce((total, duracion) => total + duracion, 0);

    return Math.round(suma / duraciones.length);
  });

  readonly resumenPorRuta = computed<ResumenRuta[]>(() => {
    const agrupado = new Map<string, number[]>();

    for (const servicio of this.serviciosFiltrados()) {
      const duracion = this.obtenerDuracionMinutos(servicio);

      if (duracion === null) {
        continue;
      }

      const nombreRuta =
        servicio.ruta?.nombre || `${servicio.origenDescripcion} → ${servicio.destinoDescripcion}`;

      const duraciones = agrupado.get(nombreRuta) || [];

      duraciones.push(duracion);

      agrupado.set(nombreRuta, duraciones);
    }

    return Array.from(agrupado.entries())
      .map(([ruta, duraciones]) => {
        const total = duraciones.reduce((suma, duracion) => suma + duracion, 0);

        return {
          ruta,
          viajesCompletados: duraciones.length,

          duracionPromedioMinutos: Math.round(total / duraciones.length),
        };
      })
      .sort((primero, segundo) => segundo.viajesCompletados - primero.viajesCompletados);
  });

  readonly resumenPorProducto = computed<ResumenProducto[]>(() => {
    const agrupado = new Map<string, number>();

    for (const servicio of this.serviciosFiltrados()) {
      const producto = this.obtenerNombreProducto(servicio.productoTransportado);

      agrupado.set(producto, (agrupado.get(producto) || 0) + 1);
    }

    return Array.from(agrupado.entries())
      .map(([producto, cantidad]) => ({
        producto,
        cantidad,
      }))
      .sort((primero, segundo) => segundo.cantidad - primero.cantidad);
  });

  constructor(
    private readonly servicioService: ServicioTransporteService,

    private readonly router: Router,

    private readonly sesionService: SesionService,
  ) {}

  ngOnInit(): void {
    this.cargarServicios();
  }

  cargarServicios(): void {
    void this.configuracion.cargar();
    this.cargando.set(true);
    this.mensajeError.set('');

    this.servicioService.listar().subscribe({
      next: (servicios) => {
        const ordenados = [...servicios].sort((primero, segundo) => segundo.id - primero.id);

        this.servicios.set(ordenados);

        this.cargando.set(false);
      },

      error: (error) => {
        this.cargando.set(false);

        if (error.status === 401) {
          this.sesionService.cerrarSesionLocal();

          return;
        }

        if (error.status === 403) {
          this.mensajeError.set('No tienes permisos para ' + 'consultar los reportes.');

          return;
        }

        this.mensajeError.set('No fue posible cargar la ' + 'información del reporte.');
      },
    });
  }

  cambiarEstado(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value as EstadoFiltro;

    this.estadoSeleccionado.set(valor);
  }

  cambiarRuta(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;

    this.rutaSeleccionada.set(valor);
  }

  cambiarChofer(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;

    this.choferSeleccionado.set(valor);
  }

  cambiarFechaDesde(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;

    this.fechaDesde.set(valor);
  }

  cambiarFechaHasta(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;

    this.fechaHasta.set(valor);
  }

  limpiarFiltros(): void {
    this.estadoSeleccionado.set('TODOS');

    this.rutaSeleccionada.set('TODAS');

    this.choferSeleccionado.set('TODOS');

    this.fechaDesde.set('');
    this.fechaHasta.set('');
  }

  formatearDuracion(minutos: number): string {
    return formatearDuracionMinutos(minutos);
  }

  obtenerNombreProducto(producto: string): string {
    const nombres: Record<string, string> = {
      RFF: 'RFF — Racimo Fruto Fresco',

      FS: 'FS — Fruto Suelto',

      ESCOBAJO: 'Escobajo',

      FIBRA: 'Fibra',

      TORTA_CLARIFICACION: 'Torta de clarificación',

      LODO_BIOGAS: 'Lodo de biogás',

      CENIZA: 'Ceniza',

      RESIDUOS_VARIOS: 'Residuos varios',
    };

    return nombres[producto] || producto.replaceAll('_', ' ');
  }

  obtenerDuracionServicio(servicio: ServicioTransporte): string {
    const duracion = this.obtenerDuracionMinutos(servicio);

    if (duracion === null) {
      return '—';
    }

    return this.formatearDuracion(duracion);
  }

  tramosReporte(servicio: ServicioTransporte) {
    return [...(servicio.tramos || [])].sort((a, b) => a.orden - b.orden);
  }

  recorridoReporte(servicio: ServicioTransporte): string {
    const tramos = this.tramosReporte(servicio);
    return tramos.length
      ? [tramos[0].origen, ...tramos.map((t) => t.destino)].join(' → ')
      : `${servicio.origenDescripcion} → ${servicio.destinoDescripcion}`;
  }

  tiemposReporte(servicio: ServicioTransporte, permanencia: boolean): string {
    const tramos = this.tramosReporte(servicio);
    const intervalos = permanencia
      ? tramos.slice(0, -1).map((t, i) => [t.llegadaReal, tramos[i + 1].salidaReal])
      : tramos.map((t) => [t.salidaReal, t.llegadaReal]);
    let segundos = 0,
      registrados = 0;
    for (const [inicio, fin] of intervalos) {
      if (!inicio || !fin) continue;
      const diferencia = (new Date(fin).getTime() - new Date(inicio).getTime()) / 1000;
      if (!Number.isFinite(diferencia) || diferencia < 0) continue;
      segundos += diferencia;
      registrados++;
    }
    if (!registrados) return 'Sin registro';
    return (
      this.formatearDuracion(segundos / 60) + (registrados < intervalos.length ? ' (parcial)' : '')
    );
  }

  controlOdometroReporte(servicio: ServicioTransporte): string {
    const tramos = this.tramosReporte(servicio);
    if (tramos[0]?.odometroNoDisponibleSalida || tramos.at(-1)?.odometroNoDisponibleLlegada)
      return (
        'Odómetro averiado; distancia estimada por ruta. ' +
        [tramos[0]?.motivoOdometroSalida, tramos.at(-1)?.motivoOdometroLlegada]
          .filter(Boolean)
          .join(' · ')
      );
    const inicio = tramos[0]?.odometroSalidaKm;
    const fin = tramos.at(-1)?.odometroLlegadaKm;
    if (inicio == null) return 'Sin registro';
    if (fin == null) return `Salida: ${inicio} km · Regreso sin registro`;
    if (fin <= inicio)
      return `Salida: ${inicio} km · Regreso: ${fin} km · Revisar: lectura final no mayor`;
    return `Salida: ${inicio} km · Regreso: ${fin} km · Real: ${Math.round((fin - inicio) * 100) / 100} km`;
  }

  async imprimirReporte(): Promise<void> {
    if (this.vistaReporte() === 'consumo' && !this.consumo().configurada) {
      await this.dialogos.informar(
        'Consulta la configuración operativa antes de imprimir las estimaciones.',
      );
      return;
    }
    if (this.vistaReporte() === 'chofer' && this.choferSeleccionado() === 'TODOS') {
      await this.dialogos.informar('Selecciona un chofer para ' + 'generar su reporte.');

      return;
    }

    if (this.serviciosFiltrados().length === 0) {
      await this.dialogos.informar('No hay servicios para los filtros aplicados.');

      return;
    }

    window.print();
  }

  obtenerPeriodoReporte(): string {
    const desde = this.fechaDesde();

    const hasta = this.fechaHasta();

    if (desde && hasta) {
      return `${this.formatearFecha(desde)}` + ` al ${this.formatearFecha(hasta)}`;
    }

    if (desde) {
      return `Desde ${this.formatearFecha(desde)}`;
    }

    if (hasta) {
      return `Hasta ${this.formatearFecha(hasta)}`;
    }

    return 'Todos los periodos';
  }

  obtenerFechaGeneracion(): string {
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'long',
      timeStyle: 'short',
    }).format(new Date());
  }

  formatearFechaHoraReporte(valor: string | null): string {
    if (!valor) {
      return 'Sin fecha';
    }

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
      return valor.slice(0, 16).replace('T', ' ');
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(fecha);
  }

  resumirObservacion(valor: string | null): string {
    if (!valor || !valor.trim()) {
      return '-';
    }

    const texto = valor.trim();
    return texto.length <= 130 ? texto : `${texto.slice(0, 127)}...`;
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irServicios(): void {
    this.router.navigate(['/servicios']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private formatearFecha(fecha: string): string {
    const partes = fecha.split('-');

    if (partes.length !== 3) {
      return fecha;
    }

    return `${partes[2]}/` + `${partes[1]}/` + `${partes[0]}`;
  }

  private obtenerDuracionMinutos(servicio: ServicioTransporte): number | null {
    if (
      servicio.estado !== 'COMPLETADO' ||
      !servicio.fechaHoraSalidaReal ||
      !servicio.fechaHoraLlegadaReal
    ) {
      return null;
    }

    const salida = new Date(servicio.fechaHoraSalidaReal).getTime();

    const llegada = new Date(servicio.fechaHoraLlegadaReal).getTime();

    const diferencia = llegada - salida;

    if (Number.isNaN(diferencia) || diferencia < 0) {
      return null;
    }

    return diferencia / 60000;
  }
}
