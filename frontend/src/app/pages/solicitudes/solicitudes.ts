import { ActivatedRoute } from '@angular/router';
import {
  ConfirmacionViajeComponent,
  ViajeAsignadoConfirmacion,
} from '../../components/confirmacion-viaje';
import { AvanceViajeComponent } from '../../components/avance-viaje';
import { FechaHoraFuturaComponent, proximoMinutoLocal } from '../../components/fecha-hora-futura';
import { ResumenSolicitudViajeComponent } from '../../components/resumen-solicitud-viaje';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RutaService, Ubicacion } from '../../services/ruta.service';
import { ItinerarioViajeComponent } from '../../components/itinerario-viaje';
import { PlanViajeRequest } from '../../services/servicio-transporte.service';

import {
  PersonaServicio,
  RutaServicio,
  VehiculoServicio,
} from '../../services/servicio-transporte.service';

import { ServicioTransporte } from '../../services/servicio-transporte.service';

import { SesionService } from '../../services/sesion.service';

import {
  EstadoSolicitud,
  PrioridadSolicitud,
  ProductoSolicitud,
  SolicitudTransporte,
  SolicitudTransporteService,
  TipoSolicitudTransporte,
} from '../../services/solicitud-transporte';

@Component({
  selector: 'app-solicitudes',
  imports: [
    ConfirmacionViajeComponent,
    AvanceViajeComponent,
    FechaHoraFuturaComponent,
    ReactiveFormsModule,
    ItinerarioViajeComponent,
    ResumenSolicitudViajeComponent,
  ],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.scss',
})
export class Solicitudes implements OnInit {
  private rutaPendiente = inject(ActivatedRoute, { optional: true });
  private pendienteAbierto = false;

  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  private readonly solicitudApi = inject(SolicitudTransporteService);

  private readonly sesionService = inject(SesionService);

  readonly solicitudes = signal<SolicitudTransporte[]>([]);

  readonly camiones = signal<VehiculoServicio[]>([]);
  readonly choferes = signal<PersonaServicio[]>([]);
  readonly copilotos = signal<PersonaServicio[]>([]);
  readonly rutas = signal<RutaServicio[]>([]);
  private readonly rutaApi = inject(RutaService);
  readonly ubicaciones = signal<Ubicacion[]>([]);
  readonly creandoRuta = signal(false);
  readonly mostrarNuevaRuta = signal(false);
  readonly itinerarioSolicitud = signal<PlanViajeRequest | null>(null);
  readonly solicitudSinRecorrido = signal<SolicitudTransporte | null>(null);
  readonly rutaAnteriorId = signal<number | null>(null);
  readonly itinerario = signal<PlanViajeRequest | null>(null);
  readonly nuevaRutaForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{3,30}$/)]],
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    origenUbicacionId: [0, Validators.min(1)],
    destinoUbicacionId: [0, Validators.min(1)],
    distanciaKm: [1, Validators.min(0.01)],
    duracionEstimadaMinutos: [30, [Validators.min(1), Validators.max(10080)]],
  });
  cargarCatalogoRutas(): void {
    forkJoin({
      rutas: this.rutaApi.listar(),
      ubicaciones: this.rutaApi.listarUbicaciones(),
    }).subscribe({
      next: (datos) => {
        this.rutas.set(datos.rutas.filter((r) => r.estado === 'ACTIVA'));
        this.ubicaciones.set(datos.ubicaciones.filter((u) => u.estado === 'ACTIVA'));
      },
      error: () =>
        this.mensajeFormulario.set('No se pudo cargar el catálogo de rutas. Vuelve a intentar.'),
    });
  }
  seleccionarRutaSolicitud(): void {
    this.itinerarioSolicitud.set(null);
    const ruta = this.rutas().find((r) => r.id === this.formularioSolicitud.controls.rutaId.value);
    this.formularioSolicitud.patchValue({
      origen: ruta?.origen ?? '',
      destino: ruta?.destino ?? '',
    });
  }
  guardarNuevaRuta(): void {
    if (this.creandoRuta()) return;
    if (this.nuevaRutaForm.invalid) {
      this.nuevaRutaForm.markAllAsTouched();
      this.mensajeFormulario.set('Completa los datos de la nueva ruta y sus ubicaciones.');
      return;
    }
    this.creandoRuta.set(true);
    this.rutaApi
      .registrar({ ...this.nuevaRutaForm.getRawValue(), estado: 'ACTIVA', observaciones: '' })
      .subscribe({
        next: (ruta) => {
          this.creandoRuta.set(false);
          this.rutas.update((rs) => [...rs, ruta]);
          if (this.mostrarFormulario()) {
            this.formularioSolicitud.controls.rutaId.setValue(ruta.id);
            this.seleccionarRutaSolicitud();
          } else if (!this.formularioProgramacion.controls.rutaId.value)
            this.formularioProgramacion.controls.rutaId.setValue(ruta.id);
          this.mostrarNuevaRuta.set(false);
          this.mensajeFormulario.set(
            'Ruta creada. Ya está disponible en el catálogo para asignarla al viaje.',
          );
        },
        error: (e: HttpErrorResponse) => {
          this.creandoRuta.set(false);
          this.mensajeFormulario.set(e.error?.mensaje ?? 'No se pudo registrar la ruta.');
        },
      });
  }

  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly procesandoId = signal<number | null>(null);
  readonly cargandoOpciones = signal(false);

  readonly mostrarFormulario = signal(false);
  readonly mostrarProgramacion = signal(false);
  readonly viajeAsignado = signal<ViajeAsignadoConfirmacion | null>(null);
  readonly mostrarDatosAprobacion = signal(false);
  readonly aprobandoSolicitud = signal(false);
  readonly solicitudConsulta = signal<SolicitudTransporte | null>(null);
  readonly viajeConsulta = signal<ServicioTransporte | null>(null);
  readonly cargandoConsulta = signal(false);
  readonly errorConsulta = signal('');
  private consultaVersion = 0;
  visualizarSolicitud(solicitud: SolicitudTransporte): void {
    this.solicitudConsulta.set(solicitud);
    this.viajeConsulta.set(null);
    this.actualizarConsulta();
    setTimeout(() => document.getElementById('consulta-cerrar')?.focus());
  }
  cerrarConsulta(): void {
    this.consultaVersion++;
    this.solicitudConsulta.set(null);
    this.viajeConsulta.set(null);
    this.cargandoConsulta.set(false);
  }
  actualizarConsulta(): void {
    const solicitud = this.solicitudConsulta();
    if (!solicitud) return;
    const version = ++this.consultaVersion;
    this.cargandoConsulta.set(true);
    this.errorConsulta.set('');
    this.viajeConsulta.set(null);
    this.solicitudApi.buscarPorId(solicitud.id).subscribe({
      next: (actualizada) => {
        if (version !== this.consultaVersion) return;
        this.solicitudConsulta.set(actualizada);
        this.reemplazarSolicitud(actualizada);
        if (!actualizada.servicioId) {
          this.cargandoConsulta.set(false);
          return;
        }
        this.http.get<ServicioTransporte>(`/api/servicios/${actualizada.servicioId}`).subscribe({
          next: (viaje) => {
            if (version !== this.consultaVersion) return;
            this.viajeConsulta.set(viaje);
            this.cargandoConsulta.set(false);
          },
          error: () => {
            if (version !== this.consultaVersion) return;
            this.cargandoConsulta.set(false);
            this.errorConsulta.set(
              'No se pudo cargar el avance del viaje. Pulsa Actualizar para intentar de nuevo.',
            );
          },
        });
      },
      error: () => {
        if (version !== this.consultaVersion) return;
        this.cargandoConsulta.set(false);
        this.errorConsulta.set(
          'No se pudo actualizar la solicitud. Se muestran los datos del listado; pulsa Actualizar para volver a consultar.',
        );
      },
    });
  }
  readonly solicitudRevision = signal<SolicitudTransporte | null>(null);
  readonly errorRevision = signal('');
  readonly mostrarMotivo = signal(false);

  readonly accionMotivo = signal<'RECHAZAR' | 'CANCELAR' | null>(null);

  readonly solicitudMotivo = signal<SolicitudTransporte | null>(null);

  readonly solicitudSeleccionada = signal<SolicitudTransporte | null>(null);

  readonly mensajeError = signal('');
  readonly mensajeExito = signal('');
  readonly mensajeFormulario = signal('');
  readonly mensajeMotivo = signal('');

  readonly filtroEstado = signal<string>('TODOS');

  readonly rol = sessionStorage.getItem('rol') || '';

  readonly solicitudesFiltradas = computed(() => {
    const estado = this.filtroEstado();

    if (estado === 'TODOS') {
      return this.solicitudes();
    }

    return this.solicitudes().filter((solicitud) => solicitud.estado === estado);
  });

  readonly totalPendientes = computed(
    () =>
      this.solicitudes().filter(
        (solicitud) => solicitud.estado === 'SOLICITADA' || solicitud.estado === 'EN_REVISION',
      ).length,
  );

  readonly totalAprobadas = computed(
    () => this.solicitudes().filter((solicitud) => solicitud.estado === 'APROBADA').length,
  );

  readonly totalProgramadas = computed(
    () => this.solicitudes().filter((solicitud) => solicitud.estado === 'PROGRAMADA').length,
  );

  readonly formularioSolicitud = this.fb.group({
    tipoSolicitud: ['RECOJO_RFF' as TipoSolicitudTransporte, Validators.required],
    solicitanteNombre: ['', Validators.required],
    areaSolicitante: ['', Validators.required],
    telefono: [''],
    rutaId: [null as number | null, Validators.required],
    origen: ['', Validators.required],
    destino: ['', Validators.required],
    productoTransportado: ['RFF' as ProductoSolicitud, Validators.required],
    descripcionProducto: [''],
    cantidadEstimadaToneladas: [null as number | null],
    fechaRequerida: ['', Validators.required],
    prioridad: ['NORMAL' as PrioridadSolicitud, Validators.required],
    observaciones: [''],
  });

  readonly formularioProgramacion = this.fb.group({
    codigoServicio: ['', Validators.required],
    vehiculoId: [null as number | null, Validators.required],
    conductorId: [null as number | null, Validators.required],
    copilotoId: [null as number | null],
    rutaId: [null as number | null, [Validators.required]],
    requiereRetorno: [false],
    fechaHoraSalida: ['', Validators.required],
    observaciones: [''],
  });

  readonly formularioMotivo = this.fb.group({
    motivo: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]],
  });

  cargaAutomatica(): boolean {
    return ['RECOJO_RFF', 'RECOJO_FS'].includes(
      this.formularioSolicitud.controls.tipoSolicitud.value || '',
    );
  }
  ajustarCargaPorTipo(): void {
    const tipo = this.formularioSolicitud.controls.tipoSolicitud.value;
    const producto = this.formularioSolicitud.controls.productoTransportado;
    if (tipo === 'RECOJO_RFF' || tipo === 'RECOJO_FS') {
      producto.setValue(tipo === 'RECOJO_RFF' ? 'RFF' : 'FS');
      producto.disable();
      this.formularioSolicitud.controls.descripcionProducto.setValue('');
    } else {
      producto.enable();
    }
  }
  ngOnInit(): void {
    this.cargarSolicitudes();
  }

  puedeEditar(): boolean {
    return ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(this.rol) || this.rol === 'OPERADOR';
  }

  cargarSolicitudes(): void {
    this.cargando.set(true);
    this.mensajeError.set('');

    this.solicitudApi.listar().subscribe({
      next: (datos) => {
        this.solicitudes.set(datos);
        const id = Number(this.rutaPendiente?.snapshot.queryParamMap.get('atender'));
        if (!this.pendienteAbierto && Number.isSafeInteger(id) && id > 0) {
          this.pendienteAbierto = true;
          const x = datos.find((v) => v.id === id);
          if (x) {
            if (x.estado === 'SOLICITADA') this.pasarARevision(x);
            else if (x.estado === 'EN_REVISION') this.aprobar(x);
            else if (x.estado === 'APROBADA') this.abrirProgramacion(x);
            else this.visualizarSolicitud(x);
          } else this.mensajeError.set('La solicitud ya no está disponible.');
        }
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        this.manejarErrorGeneral(error);
      },
    });
  }

  cambiarFiltro(evento: Event): void {
    const elemento = evento.target as HTMLSelectElement;
    this.filtroEstado.set(elemento.value);
  }

  abrirFormulario(): void {
    if (!this.puedeEditar()) {
      return;
    }

    this.itinerarioSolicitud.set(null);
    this.formularioSolicitud.reset({
      tipoSolicitud: 'RECOJO_RFF',
      solicitanteNombre: '',
      areaSolicitante: '',
      telefono: '',
      origen: '',
      destino: '',
      rutaId: null,
      productoTransportado: 'RFF',
      descripcionProducto: '',
      cantidadEstimadaToneladas: null,
      fechaRequerida: proximoMinutoLocal(),
      prioridad: 'NORMAL',
      observaciones: '',
    });

    this.ajustarCargaPorTipo();
    this.mensajeFormulario.set('');
    this.mostrarNuevaRuta.set(false);
    this.cargarCatalogoRutas();
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario(): void {
    if (this.guardando()) {
      return;
    }

    this.mostrarFormulario.set(false);
  }

  guardarSolicitud(): void {
    this.mensajeFormulario.set('');

    if (this.formularioSolicitud.invalid || !this.itinerarioSolicitud()) {
      this.formularioSolicitud.markAllAsTouched();
      this.mensajeFormulario.set(
        'Completa los campos obligatorios y calcula o ingresa los kilómetros del recorrido.',
      );
      return;
    }

    const datos = this.formularioSolicitud.getRawValue();

    const fecha = new Date(datos.fechaRequerida!);

    if (Number.isNaN(fecha.getTime()) || fecha.getTime() <= Date.now()) {
      this.mensajeFormulario.set('La fecha requerida debe ser futura.');
      return;
    }

    if (datos.productoTransportado === 'RESIDUOS_VARIOS' && !datos.descripcionProducto?.trim()) {
      this.mensajeFormulario.set('Describe los residuos que serán transportados.');
      return;
    }

    if (datos.cantidadEstimadaToneladas !== null && datos.cantidadEstimadaToneladas <= 0) {
      this.mensajeFormulario.set('La cantidad estimada debe ser mayor que cero.');
      return;
    }

    this.guardando.set(true);

    this.solicitudApi
      .registrar({
        rutaId: datos.rutaId!,
        planViaje: this.itinerarioSolicitud()!,
        tipoSolicitud: datos.tipoSolicitud as TipoSolicitudTransporte,
        solicitanteNombre: datos.solicitanteNombre!.trim(),
        areaSolicitante: datos.areaSolicitante!.trim(),
        telefono: datos.telefono?.trim() || null,
        origen: datos.origen!.trim(),
        destino: datos.destino!.trim(),
        productoTransportado: datos.productoTransportado as ProductoSolicitud,
        descripcionProducto: datos.descripcionProducto?.trim() || null,
        cantidadEstimadaToneladas: datos.cantidadEstimadaToneladas,
        fechaRequerida: `${datos.fechaRequerida}:00`,
        prioridad: datos.prioridad as PrioridadSolicitud,
        observaciones: datos.observaciones?.trim() || null,
      })
      .subscribe({
        next: (nueva) => {
          this.solicitudes.update((lista) => [nueva, ...lista]);

          this.guardando.set(false);
          this.mostrarFormulario.set(false);
          this.mensajeExito.set(`La solicitud ${nueva.codigo} se registró correctamente.`);
        },
        error: (error: HttpErrorResponse) => {
          this.guardando.set(false);
          this.mensajeFormulario.set(
            this.obtenerMensaje(error, 'No fue posible registrar la solicitud.'),
          );
        },
      });
  }

  pasarARevision(solicitud: SolicitudTransporte): void {
    this.abrirRevision(solicitud);
  }
  aprobar(solicitud: SolicitudTransporte): void {
    this.abrirRevision(solicitud);
    this.aprobandoSolicitud.set(true);
  }
  abrirRevision(solicitud: SolicitudTransporte): void {
    if (this.procesandoId() !== null) return;
    this.mostrarDatosAprobacion.set(false);
    this.aprobandoSolicitud.set(false);
    this.errorRevision.set('');
    this.solicitudRevision.set(solicitud);
    setTimeout(() => document.getElementById('revision-cerrar')?.focus());
  }
  cerrarRevision(): void {
    if (this.procesandoId() === null) this.solicitudRevision.set(null);
  }
  confirmarRevision(): void {
    const solicitud = this.solicitudRevision();
    if (!solicitud) return;
    if (!this.aprobandoSolicitud() && solicitud.estado === 'SOLICITADA')
      this.actualizarEstado(solicitud, 'EN_REVISION', null);
    else if (this.aprobandoSolicitud() && solicitud.estado === 'EN_REVISION' && solicitud.planViaje)
      this.actualizarEstado(solicitud, 'APROBADA', null);
  }
  rechazarRevision(): void {
    const solicitud = this.solicitudRevision();
    if (!solicitud || this.procesandoId() !== null) return;
    this.solicitudRevision.set(null);
    this.rechazar(solicitud);
  }
  completarDesdeRevision(): void {
    const solicitud = this.solicitudRevision();
    if (!solicitud || this.procesandoId() !== null) return;
    this.solicitudRevision.set(null);
    this.abrirRecorridoAnterior(solicitud);
  }
  rechazar(solicitud: SolicitudTransporte): void {
    this.abrirMotivo(solicitud, 'RECHAZAR');
  }

  cancelar(solicitud: SolicitudTransporte): void {
    this.abrirMotivo(solicitud, 'CANCELAR');
  }

  private abrirMotivo(solicitud: SolicitudTransporte, accion: 'RECHAZAR' | 'CANCELAR'): void {
    if (this.procesandoId() !== null) {
      return;
    }

    this.solicitudMotivo.set(solicitud);
    this.accionMotivo.set(accion);
    this.mensajeMotivo.set('');
    this.formularioMotivo.reset({
      motivo: '',
    });
    this.mostrarMotivo.set(true);
  }

  cerrarMotivo(): void {
    if (this.procesandoId() !== null) {
      return;
    }

    this.mostrarMotivo.set(false);
    this.accionMotivo.set(null);
    this.solicitudMotivo.set(null);
    this.mensajeMotivo.set('');
    this.formularioMotivo.reset();
  }

  confirmarMotivo(): void {
    const solicitud = this.solicitudMotivo();
    const accion = this.accionMotivo();

    if (!solicitud || !accion) {
      return;
    }

    if (this.formularioMotivo.invalid) {
      this.formularioMotivo.markAllAsTouched();
      this.mensajeMotivo.set('Escribe un motivo de al menos 3 caracteres.');
      return;
    }

    const motivo = this.formularioMotivo.controls.motivo.value?.trim();

    if (!motivo) {
      return;
    }

    this.mensajeMotivo.set('');

    this.actualizarEstado(solicitud, accion === 'CANCELAR' ? 'CANCELADA' : 'RECHAZADA', motivo);
  }

  private actualizarEstado(
    solicitud: SolicitudTransporte,
    estado: EstadoSolicitud,
    motivo: string | null,
  ): void {
    if (this.procesandoId() !== null) {
      return;
    }

    this.procesandoId.set(solicitud.id);
    this.errorRevision.set('');
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.solicitudApi.cambiarEstado(solicitud.id, estado, motivo).subscribe({
      next: (actualizada) => {
        this.reemplazarSolicitud(actualizada);
        this.procesandoId.set(null);
        if (this.solicitudRevision()?.id === actualizada.id) {
          this.solicitudRevision.set(null);
        }

        if (this.solicitudMotivo()?.id === actualizada.id) {
          this.cerrarMotivo();
        }

        this.mensajeExito.set(
          `La solicitud ${actualizada.codigo} cambió a ${this.nombreEstado(actualizada.estado)}.`,
        );
      },
      error: (error: HttpErrorResponse) => {
        this.procesandoId.set(null);

        const mensaje = this.obtenerMensaje(error, 'No fue posible actualizar la solicitud.');

        if (this.solicitudRevision()?.id === solicitud.id) {
          this.errorRevision.set(mensaje);
        } else if (this.mostrarMotivo()) {
          this.mensajeMotivo.set(mensaje);
        } else {
          this.mensajeError.set(mensaje);
        }
      },
    });
  }

  abrirRecorridoAnterior(solicitud: SolicitudTransporte): void {
    this.solicitudSinRecorrido.set(solicitud);
    this.rutaAnteriorId.set(solicitud.rutaId);
    this.itinerarioSolicitud.set(null);
    this.mensajeFormulario.set('');
    this.cargarCatalogoRutas();
  }
  guardarRecorridoAnterior(): void {
    const solicitud = this.solicitudSinRecorrido(),
      plan = this.itinerarioSolicitud(),
      rutaId = this.rutaAnteriorId();
    if (!solicitud || !plan || !rutaId || this.guardando()) return;
    this.guardando.set(true);
    this.mensajeFormulario.set('');
    this.solicitudApi.completarRecorrido(solicitud.id, rutaId, plan).subscribe({
      next: (actualizada) => {
        this.reemplazarSolicitud(actualizada);
        this.guardando.set(false);
        this.solicitudSinRecorrido.set(null);
        this.mensajeExito.set(
          'Recorrido guardado. La solicitud está en revisión y debe aprobarse antes de programar.',
        );
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeFormulario.set(this.obtenerMensaje(error, 'No se pudo guardar el recorrido.'));
      },
    });
  }
  abrirProgramacion(solicitud: SolicitudTransporte): void {
    if (!this.puedeEditar() || solicitud.estado !== 'APROBADA') {
      return;
    }

    this.mostrarNuevaRuta.set(false);
    if (!solicitud.planViaje) {
      this.abrirRecorridoAnterior(solicitud);
      return;
    }
    this.itinerario.set(solicitud.planViaje);
    this.solicitudSeleccionada.set(solicitud);

    this.formularioProgramacion.reset({
      codigoServicio: this.generarCodigoServicio(solicitud.codigo),
      vehiculoId: null,
      conductorId: null,
      copilotoId: null,
      rutaId: solicitud.rutaId ?? null,
      requiereRetorno: false,
      fechaHoraSalida:
        solicitud.fechaRequerida?.slice(0, 16) &&
        solicitud.fechaRequerida.slice(0, 16) > proximoMinutoLocal()
          ? solicitud.fechaRequerida.slice(0, 16)
          : proximoMinutoLocal(),
      observaciones: '',
    });

    this.mensajeFormulario.set('');
    this.mostrarProgramacion.set(true);
    this.cargarOpcionesProgramacion();
  }

  private cargarOpcionesProgramacion(): void {
    this.cargandoOpciones.set(true);

    forkJoin({
      vehiculos: this.http.get<VehiculoServicio[]>('/api/vehiculos'),
      personas: this.http.get<PersonaServicio[]>('/api/conductores'),
      ubicaciones: this.rutaApi.listarUbicaciones(),
      rutas: this.http.get<RutaServicio[]>('/api/rutas'),
      servicios: this.http.get<ServicioTransporte[]>('/api/servicios'),
    }).subscribe({
      next: ({ vehiculos, personas, ubicaciones, rutas, servicios }) => {
        const serviciosActivos = servicios.filter(
          (servicio) => servicio.estado === 'PROGRAMADO' || servicio.estado === 'EN_CURSO',
        );

        const vehiculosOcupados = new Set(serviciosActivos.map((servicio) => servicio.vehiculo.id));

        const personasOcupadas = new Set<number>();

        serviciosActivos.forEach((servicio) => {
          personasOcupadas.add(servicio.conductor.id);

          if (servicio.copiloto) {
            personasOcupadas.add(servicio.copiloto.id);
          }
        });

        this.camiones.set(
          vehiculos.filter(
            (vehiculo) =>
              vehiculo.tipo === 'CAMION' &&
              vehiculo.estado === 'DISPONIBLE' &&
              !vehiculosOcupados.has(vehiculo.id),
          ),
        );

        this.choferes.set(
          personas.filter(
            (persona) =>
              persona.estado === 'DISPONIBLE' &&
              (persona.rolTripulacion === 'CHOFER' || persona.rolTripulacion === 'AMBOS') &&
              !personasOcupadas.has(persona.id),
          ),
        );

        this.copilotos.set(
          personas.filter(
            (persona) =>
              persona.estado === 'DISPONIBLE' &&
              (persona.rolTripulacion === 'COPILOTO' || persona.rolTripulacion === 'AMBOS') &&
              !personasOcupadas.has(persona.id),
          ),
        );

        this.ubicaciones.set(ubicaciones.filter((u) => u.estado === 'ACTIVA'));
        this.rutas.set(rutas.filter((ruta) => ruta.estado === 'ACTIVA'));

        this.cargandoOpciones.set(false);

        if (
          this.camiones().length === 0 ||
          this.choferes().length === 0 ||
          this.rutas().length === 0
        ) {
          this.mensajeFormulario.set(
            'No existen suficientes recursos disponibles para programar el servicio.',
          );
        }
      },
      error: (error: HttpErrorResponse) => {
        this.cargandoOpciones.set(false);
        this.mensajeFormulario.set(
          this.obtenerMensaje(error, 'No fue posible cargar los recursos disponibles.'),
        );
      },
    });
  }

  cerrarProgramacion(): void {
    if (this.guardando()) {
      return;
    }

    this.mostrarProgramacion.set(false);
    this.solicitudSeleccionada.set(null);
  }

  programarSolicitud(): void {
    const solicitud = this.solicitudSeleccionada();

    if (!solicitud || this.guardando() || this.viajeAsignado()) {
      return;
    }

    this.mensajeFormulario.set('');

    if (this.formularioProgramacion.invalid || !this.itinerario()) {
      this.formularioProgramacion.markAllAsTouched();
      this.mensajeFormulario.set('Completa los campos obligatorios.');
      return;
    }

    const datos = this.formularioProgramacion.getRawValue();

    if (
      solicitud.fechaRequerida &&
      datos.fechaHoraSalida! < solicitud.fechaRequerida.slice(0, 16)
    ) {
      this.mensajeFormulario.set(
        'La salida no puede ser anterior al horario requerido en la solicitud.',
      );
      return;
    }

    if (datos.conductorId === datos.copilotoId) {
      this.mensajeFormulario.set('El chofer y el copiloto deben ser personas distintas.');
      return;
    }

    const fecha = new Date(datos.fechaHoraSalida!);

    if (Number.isNaN(fecha.getTime()) || fecha.getTime() <= Date.now()) {
      this.mensajeFormulario.set('La fecha de salida debe ser futura.');
      return;
    }

    this.guardando.set(true);

    const persona = this.choferes().find((c) => c.id === datos.conductorId);
    const primero = (texto: string | undefined) => texto?.trim().split(/\s+/)[0] ?? '';
    const chofer =
      [primero(persona?.nombres), primero(persona?.apellidos)].filter(Boolean).join(' ') ||
      'Chofer seleccionado';
    const vehiculoAsignado = this.camiones().find((v) => v.id === datos.vehiculoId);
    const unidad = vehiculoAsignado?.placa?.trim() || 'Unidad seleccionada';
    const ubicaciones = new Map(this.ubicaciones().map((u) => [u.id, u.nombre]));

    this.solicitudApi
      .programar(solicitud.id, {
        codigoServicio: datos.codigoServicio!.trim(),
        vehiculoId: datos.vehiculoId!,
        conductorId: datos.conductorId!,
        copilotoId: null,
        rutaId: datos.rutaId!,
        tramos: [],
        requiereRetorno: true,
        fechaHoraSalida: `${datos.fechaHoraSalida}:00`,
        observaciones: datos.observaciones?.trim() || null,
      })
      .subscribe({
        next: (actualizada) => {
          this.reemplazarSolicitud(actualizada);
          const plan = actualizada.planViaje ?? solicitud.planViaje;
          const recorrido =
            actualizada.resumenRecorrido?.trim() ||
            (plan
              ? [
                  plan.inicioUbicacionId,
                  ...plan.paradas.map((p) => p.ubicacionId),
                  plan.plantaRetornoId,
                ]
                  .map((id) => ubicaciones.get(id) || `Punto #${id}`)
                  .join(' → ')
              : `${solicitud.origen} → ${solicitud.destino}`);
          this.viajeAsignado.set({
            codigo: actualizada.servicioCodigo?.trim() || 'Código no disponible',
            chofer,
            unidad,
            recorrido,
          });
          this.guardando.set(false);
          this.mostrarProgramacion.set(false);
          this.solicitudSeleccionada.set(null);

          this.mensajeExito.set(
            `La solicitud ${actualizada.codigo} generó el servicio ${actualizada.servicioCodigo}.`,
          );
        },
        error: (error: HttpErrorResponse) => {
          this.guardando.set(false);
          this.mensajeFormulario.set(
            this.obtenerMensaje(error, 'No fue posible programar la solicitud.'),
          );
        },
      });
  }

  private reemplazarSolicitud(actualizada: SolicitudTransporte): void {
    this.solicitudes.update((lista) =>
      lista.map((item) => (item.id === actualizada.id ? actualizada : item)),
    );
  }

  nombreTipo(tipo: TipoSolicitudTransporte): string {
    const nombres: Record<TipoSolicitudTransporte, string> = {
      RECOJO_RFF: 'Recojo de RFF',
      RECOJO_FS: 'Recojo de fruto suelto',
      MOVIMIENTO_INTERNO: 'Movimiento interno',
      DISPOSICION_SUBPRODUCTO: 'Disposición de subproducto',
      OTRO: 'Otro',
    };

    return nombres[tipo];
  }

  nombreEstado(estado: EstadoSolicitud): string {
    const nombres: Record<EstadoSolicitud, string> = {
      SOLICITADA: 'Solicitada',
      EN_REVISION: 'En revisión',
      APROBADA: 'Aprobada',
      RECHAZADA: 'Rechazada',
      PROGRAMADA: 'Programada',
      EN_CURSO: 'En curso',
      COMPLETADA: 'Completada',
      INTERRUMPIDA: 'Interrumpida por avería',
      CANCELADA: 'Cancelada',
    };

    return nombres[estado];
  }

  nombreProducto(producto: ProductoSolicitud): string {
    const nombres: Record<ProductoSolicitud, string> = {
      RFF: 'RFF — Racimo de Fruto Fresco',
      FS: 'FS — Fruto Fresco',
      ESCOBAJO: 'Escobajo',
      FIBRA: 'Fibra',
      TORTA_CLARIFICACION: 'Torta de clarificación',
      LODO_BIOGAS: 'Lodo de biogás',
      CENIZA: 'Ceniza',
      RESIDUOS_VARIOS: 'Residuos varios',
    };

    return nombres[producto];
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private fechaLocalFutura(dias: number, hora: number): string {
    const fecha = new Date();

    fecha.setDate(fecha.getDate() + dias);
    fecha.setHours(hora, 0, 0, 0);

    const dosDigitos = (valor: number) => String(valor).padStart(2, '0');

    return (
      [fecha.getFullYear(), dosDigitos(fecha.getMonth() + 1), dosDigitos(fecha.getDate())].join(
        '-',
      ) +
      'T' +
      [dosDigitos(fecha.getHours()), dosDigitos(fecha.getMinutes())].join(':')
    );
  }

  private manejarErrorGeneral(error: HttpErrorResponse): void {
    if (error.status === 401) {
      this.sesionService.cerrarSesionLocal();
      return;
    }

    if (error.status === 403) {
      this.mensajeError.set(
        error.error?.mensaje ||
          error.error?.message ||
          'No tienes permisos para realizar esta operación.',
      );

      return;
    }

    this.mensajeError.set(this.obtenerMensaje(error, 'No fue posible completar la operación.'));
  }

  private obtenerMensaje(error: HttpErrorResponse, predeterminado: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error.trim();
    }

    return error.error?.mensaje || error.error?.message || error.error?.detail || predeterminado;
  }

  private generarCodigoServicio(codigoSolicitud: string): string {
    if (codigoSolicitud.startsWith('SOL-')) {
      return `SER-${codigoSolicitud.substring(4)}`;
    }

    return `SER-${codigoSolicitud}`;
  }
}
