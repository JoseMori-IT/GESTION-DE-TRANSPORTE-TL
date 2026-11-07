import { PendientesOperativosComponent } from '../../components/pendientes-operativos';
import { COLORES_SERVICIO } from '../../services/colores-estados';
import { GraficoBarrasComponent, BarraResumen } from '../../components/grafico-barras';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ConductorService } from '../../services/conductor.service';

import {
  ServicioTransporte,
  ServicioTransporteService,
} from '../../services/servicio-transporte.service';

import { VehiculoService } from '../../services/vehiculo.service';

import { MantenimientoService } from '../../services/mantenimiento.service';

import { SesionService } from '../../services/sesion.service';

interface Perfil {
  correo: string;
  nombre: string;
  nombreCorto: string;
  rol: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, GraficoBarrasComponent, PendientesOperativosComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  readonly perfil = signal<Perfil | null>(null);

  readonly cargando = signal(true);
  readonly mensajeError = signal('');

  /*
   * Se conservan porque podrían estar siendo utilizados
   * en el HTML actual del dashboard.
   */
  readonly totalVehiculos = signal(0);
  readonly totalConductores = signal(0);

  readonly totalCamiones = signal(0);
  readonly camionesDisponibles = signal(0);
  readonly choferesDisponibles = signal(0);

  readonly serviciosProgramados = signal(0);
  readonly serviciosEnCurso = signal(0);
  readonly serviciosCompletados = signal(0);
  readonly mantenimientosPendientes = signal(0);

  readonly barrasServicios = signal<BarraResumen[]>([]);
  readonly barrasCamiones = signal<BarraResumen[]>([]);
  readonly totalServicios = signal(0);
  readonly actualizadoGraficos = signal('');
  readonly serviciosRecientes = signal<ServicioTransporte[]>([]);

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly vehiculoService: VehiculoService,
    private readonly conductorService: ConductorService,
    private readonly servicioService: ServicioTransporteService,
    private readonly mantenimientoService: MantenimientoService,
    private readonly sesionService: SesionService,
  ) {}

  ngOnInit(): void {
    this.cargarDashboard();
  }

  cargarDashboard(): void {
    this.cargando.set(true);
    this.mensajeError.set('');

    forkJoin({
      perfil: this.http.get<Perfil>('/api/perfil'),
      vehiculos: this.vehiculoService.listar(),
      conductores: this.conductorService.listar(),
      servicios: this.servicioService.listar(),
      mantenimientos: this.mantenimientoService.listar(),
    }).subscribe({
      next: ({ perfil, vehiculos, conductores, servicios, mantenimientos }) => {
        this.perfil.set(perfil);

        this.totalVehiculos.set(vehiculos.length);

        const camiones = vehiculos.filter(
          (vehiculo) =>
            vehiculo.tipo === 'CAMION' &&
            !['INACTIVO', 'BAJA_DEFINITIVA'].includes(vehiculo.estado),
        );

        this.barrasCamiones.set([
          {
            nombre: 'Disponibles',
            valor: camiones.filter((c) => c.estado === 'DISPONIBLE').length,
            color: COLORES_SERVICIO.COMPLETADO,
          },
          {
            nombre: 'En servicio',
            valor: camiones.filter((c) => c.estado === 'EN_SERVICIO').length,
            color: COLORES_SERVICIO.EN_CURSO,
          },
          {
            nombre: 'Mantenimiento',
            valor: camiones.filter((c) => c.estado === 'MANTENIMIENTO').length,
            color: '#e99a24',
          },
        ]);
        this.totalServicios.set(servicios.length);
        this.barrasServicios.set([
          {
            nombre: 'Programados',
            valor: servicios.filter((s) => s.estado === 'PROGRAMADO').length,
            color: COLORES_SERVICIO.PROGRAMADO,
          },
          {
            nombre: 'En curso',
            valor: servicios.filter((s) => s.estado === 'EN_CURSO').length,
            color: COLORES_SERVICIO.EN_CURSO,
          },
          {
            nombre: 'Completados',
            valor: servicios.filter((s) => s.estado === 'COMPLETADO').length,
            color: COLORES_SERVICIO.COMPLETADO,
          },
          {
            nombre: 'Cancelados',
            valor: servicios.filter((s) => s.estado === 'CANCELADO').length,
            color: COLORES_SERVICIO.CANCELADO,
          },
          {
            nombre: 'Interrumpidos',
            valor: servicios.filter((s) => s.estado === 'INTERRUMPIDO').length,
            color: '#e99a24',
          },
        ]);
        this.actualizadoGraficos.set(
          new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }),
        );
        this.totalCamiones.set(camiones.length);

        this.camionesDisponibles.set(
          camiones.filter((camion) => camion.estado === 'DISPONIBLE').length,
        );

        const personalActivo = conductores.filter(
          (persona) => persona.estado !== 'INACTIVO' && persona.estado !== 'SUSPENDIDO',
        );

        this.totalConductores.set(personalActivo.length);

        this.choferesDisponibles.set(
          conductores.filter(
            (persona) =>
              persona.estado === 'DISPONIBLE' &&
              (persona.rolTripulacion === 'CHOFER' || persona.rolTripulacion === 'AMBOS'),
          ).length,
        );

        this.serviciosProgramados.set(
          servicios.filter((servicio) => servicio.estado === 'PROGRAMADO').length,
        );

        this.serviciosEnCurso.set(
          servicios.filter((servicio) => servicio.estado === 'EN_CURSO').length,
        );

        this.serviciosCompletados.set(
          servicios.filter((servicio) => servicio.estado === 'COMPLETADO').length,
        );

        this.mantenimientosPendientes.set(
          mantenimientos.filter(
            (mantenimiento) =>
              mantenimiento.estado === 'REPORTADO' ||
              mantenimiento.estado === 'EN_EVALUACION' ||
              mantenimiento.estado === 'EN_MANTENIMIENTO' ||
              mantenimiento.estado === 'REPARADO',
          ).length,
        );

        const ordenados = [...servicios].sort((primero, segundo) => segundo.id - primero.id);

        this.serviciosRecientes.set(ordenados.slice(0, 5));

        this.cargando.set(false);
      },
      error: (error) => {
        this.cargando.set(false);

        if (error.status === 401 || error.status === 403) {
          this.sesionService.cerrarSesionLocal();
          return;
        }

        this.mensajeError.set('No fue posible cargar la información del panel.');
      },
    });
  }

  irVehiculos(): void {
    this.router.navigate(['/vehiculos']);
  }

  irConductores(): void {
    this.router.navigate(['/conductores']);
  }

  irRutas(): void {
    this.router.navigate(['/rutas']);
  }

  irServicios(): void {
    this.router.navigate(['/servicios']);
  }

  irSolicitudes(): void {
    this.router.navigate(['/solicitudes']);
  }

  irFlota(): void {
    this.router.navigate(['/flota']);
  }

  irMantenimiento(): void {
    this.router.navigate(['/mantenimiento']);
  }

  irPersonalAdministrativo(): void {
    this.router.navigate(['/personal-administrativo']);
  }

  irReportes(): void {
    this.router.navigate(['/reportes']);
  }

  irAuditoria(): void {
    this.router.navigate(['/auditoria']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }
}
