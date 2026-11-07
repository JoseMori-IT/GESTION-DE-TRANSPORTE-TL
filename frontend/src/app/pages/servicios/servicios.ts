import { ActivatedRoute } from '@angular/router';
import { abrirVistaPreviaEvidencia } from '../../services/vista-previa-evidencia';
import { IncidenciaViajeComponent } from '../../components/incidencia-viaje';
import { HttpClient } from '@angular/common/http';
import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Router } from '@angular/router';
import { AvanceViajeComponent } from '../../components/avance-viaje';

import {
  EvidenciaServicio,
  EvidenciaServicioService,
  TipoEvidencia,
} from '../../services/evidencia-servicio.service';

import {
  EncuestaServicioResponse,
  EncuestaServicioService,
} from '../../services/encuesta-servicio.service';

import {
  EstadoServicio,
  ServicioTransporte,
  ServicioTransporteService,
} from '../../services/servicio-transporte.service';

import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-servicios',
  imports: [ReactiveFormsModule, AvanceViajeComponent, IncidenciaViajeComponent],
  templateUrl: './servicios.html',
  styleUrls: ['./servicios.scss', './servicios-apple.scss'],
})
export class Servicios implements OnInit, OnDestroy {
  private rutaPendiente = inject(ActivatedRoute, { optional: true });
  private pendienteAbierto = false;

  private readonly dialogos = inyectarDialogos(DialogosService);
  private readonly validacionHttp = inject(HttpClient);
  readonly validaciones = signal<ValidacionOperativa[]>([]);
  readonly cargandoValidaciones = signal(true);
  readonly guardandoValidacion = signal(false);
  readonly errorValidacion = signal('');
  readonly comentarioValidacion = signal('');
  puedeValidar(): boolean {
    return ['MASTER', 'COORDINADOR', 'ADMINISTRADOR', 'OPERADOR'].includes(
      sessionStorage.getItem('rol') || '',
    );
  }
  historialValidacion(id: number): ValidacionOperativa[] {
    return this.validaciones().filter((v) => v.servicioId === id);
  }
  estadoValidacion(id: number): string {
    if (this.cargandoValidaciones()) return 'Consultando validación';
    if (this.errorValidacion()) return 'Validación no disponible';
    const estado = this.historialValidacion(id)[0]?.estado;
    return estado === 'VALIDADO'
      ? 'Validado'
      : estado === 'OBSERVADO'
        ? 'Observado'
        : 'Pendiente de validación';
  }
  cargarValidaciones(): void {
    if (this.guardandoValidacion()) return;
    this.cargandoValidaciones.set(true);
    this.validacionHttp.get<ValidacionOperativa[]>('/api/validaciones-viaje').subscribe({
      next: (datos) => {
        this.validaciones.set([...datos].sort((a, b) => b.id - a.id));
        this.errorValidacion.set('');
        this.cargandoValidaciones.set(false);
      },
      error: () => {
        this.errorValidacion.set('No se pudo consultar la validación. Actualiza para reintentar.');
        this.cargandoValidaciones.set(false);
      },
    });
  }
  async validarViaje(
    servicio: ServicioTransporte,
    estado: 'VALIDADO' | 'OBSERVADO',
  ): Promise<void> {
    if (
      !this.puedeValidar() ||
      this.guardandoValidacion() ||
      this.cargandoValidaciones() ||
      servicio.estado !== 'COMPLETADO'
    )
      return;
    const comentario = this.comentarioValidacion().trim();
    if (estado === 'OBSERVADO' && !comentario) return;
    if (
      !(await this.dialogos.confirmar(
        estado === 'VALIDADO'
          ? '¿Confirmas que revisaste el recorrido y el cumplimiento del viaje?'
          : '¿Registrar esta observación del viaje?',
      ))
    )
      return;
    this.guardandoValidacion.set(true);
    this.validacionHttp
      .post<ValidacionOperativa>('/api/validaciones-viaje', {
        servicioId: servicio.id,
        estado,
        comentario,
        ultimaValidacionId: this.historialValidacion(servicio.id)[0]?.id ?? null,
      })
      .subscribe({
        next: (registro) => {
          this.validaciones.update((datos) => [registro, ...datos]);
          this.comentarioValidacion.set('');
          this.guardandoValidacion.set(false);
          this.errorValidacion.set('');
        },
        error: (error) => {
          this.guardandoValidacion.set(false);
          this.mensajeError.set(
            error.error?.mensaje ||
              error.error?.message ||
              'No se pudo registrar la validación. Actualiza y vuelve a intentarlo.',
          );
        },
      });
  }

  private actualizacion?: ReturnType<typeof setInterval>;
  readonly avisoActualizacion = signal('');
  private readonly router = inject(Router);
  private readonly formularioBuilder = inject(FormBuilder);

  private readonly servicioApi = inject(ServicioTransporteService);

  private readonly evidenciaApi = inject(EvidenciaServicioService);

  private readonly encuestaApi = inject(EncuestaServicioService);

  private readonly sesionService = inject(SesionService);

  readonly servicios = signal<ServicioTransporte[]>([]);

  readonly detalleServicio = signal<ServicioTransporte | null>(null);
  abrirDetalle(servicio: ServicioTransporte): void {
    this.detalleServicio.set(servicio);
    this.comentarioValidacion.set('');
    this.cargarServicios(true);
    setTimeout(() => document.getElementById('cerrar-detalle-servicio')?.focus());
  }
  actualizarIncidencia(actualizado: ServicioTransporte) {
    this.detalleServicio.set(actualizado);
    this.cargarServicios(true);
  }
  cerrarDetalle(): void {
    this.detalleServicio.set(null);
  }
  readonly busqueda = signal('');
  readonly filtroEstado = signal('TODOS');
  readonly estadosListado = [
    { id: 'TODOS', nombre: 'Todos' },
    { id: 'PROGRAMADO', nombre: 'Programados' },
    { id: 'EN_CURSO', nombre: 'En curso' },
    { id: 'COMPLETADO', nombre: 'Completados' },
    { id: 'CANCELADO', nombre: 'Cancelados' },
    { id: 'INTERRUMPIDO', nombre: 'Interrumpidos' },
  ];
  conteoEstado(estado: string): number {
    return estado === 'TODOS'
      ? this.servicios().length
      : this.servicios().filter((s) => s.estado === estado).length;
  }
  readonly serviciosFiltrados = computed(() => {
    const normalizar = (s: string) =>
      s
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();
    const q = normalizar(this.busqueda().trim());
    return this.servicios().filter(
      (s) =>
        (this.filtroEstado() === 'TODOS' || s.estado === this.filtroEstado()) &&
        (!q ||
          normalizar(
            [
              s.codigo,
              s.vehiculo.placa,
              s.conductor.nombres,
              s.conductor.apellidos,
              s.ruta?.nombre,
              s.origenDescripcion,
              s.destinoDescripcion,
              s.productoTransportado,
            ]
              .filter(Boolean)
              .join(' '),
          ).includes(q)),
    );
  });
  readonly cargando = signal(false);
  readonly mensajeError = signal('');
  readonly mensajeExito = signal('');
  readonly mensajeAccion = signal('');

  readonly procesandoId = signal<number | null>(null);

  /*
   * Evidencias digitales.
   */
  readonly mostrarEvidencias = signal(false);

  readonly servicioSeleccionado = signal<ServicioTransporte | null>(null);

  readonly evidencias = signal<EvidenciaServicio[]>([]);

  readonly cargandoEvidencias = signal(false);
  readonly guardandoEvidencia = signal(false);
  readonly obteniendoUbicacion = signal(false);

  readonly mensajeEvidencia = signal('');
  readonly archivoSeleccionado = signal<File | null>(null);

  readonly nombreArchivo = signal('');

  readonly formularioEvidencia = this.formularioBuilder.group({
    tipo: this.formularioBuilder.control<TipoEvidencia>('FOTO_INICIO', {
      nonNullable: true,
      validators: [Validators.required],
    }),

    descripcion: this.formularioBuilder.control('', {
      nonNullable: true,
    }),

    latitud: this.formularioBuilder.control<number | null>(null),

    longitud: this.formularioBuilder.control<number | null>(null),

    nombreResponsable: this.formularioBuilder.control('', {
      nonNullable: true,
    }),

    documentoResponsable: this.formularioBuilder.control('', {
      nonNullable: true,
    }),
  });

  /*
   * Encuestas de servicio.
   */
  readonly mostrarEncuesta = signal(false);

  readonly servicioEncuesta = signal<ServicioTransporte | null>(null);

  readonly encuestaActual = signal<EncuestaServicioResponse | null>(null);

  readonly cargandoEncuesta = signal(false);
  readonly guardandoEncuesta = signal(false);
  readonly mensajeEncuesta = signal('');

  readonly formularioEncuesta = this.formularioBuilder.group({
    puntualidad: this.formularioBuilder.control(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),

    cumplimiento: this.formularioBuilder.control(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),

    atencion: this.formularioBuilder.control(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),

    satisfaccionGeneral: this.formularioBuilder.control(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),

    comentarios: this.formularioBuilder.control('', {
      nonNullable: true,
      validators: [Validators.maxLength(1000)],
    }),
  });

  ngOnInit(): void {
    this.cargarServicios();
    this.actualizacion = setInterval(() => {
      if (!document.hidden && !this.cargando() && this.procesandoId() === null)
        this.cargarServicios(true);
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.actualizacion) clearInterval(this.actualizacion);
  }

  cargarServicios(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
      this.mensajeError.set('');
    }

    this.cargarValidaciones();
    this.servicioApi.listar().subscribe({
      next: (datos) => {
        const ordenados = [...datos].sort((primero, segundo) => segundo.id - primero.id);

        this.servicios.set(ordenados);
        const id = Number(this.rutaPendiente?.snapshot.queryParamMap.get('atender'));
        if (!this.pendienteAbierto && Number.isSafeInteger(id) && id > 0) {
          this.pendienteAbierto = true;
          const x = ordenados.find((v) => v.id === id);
          if (x) this.abrirDetalle(x);
          else this.mensajeError.set('El servicio ya no está disponible.');
        }
        const evidenciaAbierta = this.servicioSeleccionado();
        if (evidenciaAbierta) {
          const actual = ordenados.find((s) => s.id === evidenciaAbierta.id);
          if (actual) this.servicioSeleccionado.set(actual);
        }
        const detalle = this.detalleServicio();
        if (detalle) {
          const actual = ordenados.find((s) => s.id === detalle.id);
          if (actual) this.detalleServicio.set(actual);
        }
        this.avisoActualizacion.set(
          'Última consulta: ' +
            new Date().toLocaleTimeString('es-PE') +
            '. El avance se actualiza al recibir las marcaciones del chofer.',
        );
        this.cargando.set(false);
      },
      error: (error) => {
        this.cargando.set(false);
        if (silencioso && error.status !== 401 && error.status !== 403) {
          this.avisoActualizacion.set(
            'No se pudo actualizar. Se conserva el último avance recibido.',
          );
          return;
        }

        if (error.status === 401) {
          this.sesionService.cerrarSesionLocal();
          return;
        }

        this.mensajeError.set(
          error.error?.mensaje || error.error?.message || 'No fue posible cargar los servicios.',
        );
      },
    });
  }

  async cambiarEstado(servicio: ServicioTransporte, nuevoEstado: EstadoServicio): Promise<void> {
    if (this.procesandoId() !== null) {
      return;
    }

    const accion =
      nuevoEstado === 'EN_CURSO'
        ? 'registrar la salida con carga'
        : nuevoEstado === 'COMPLETADO'
          ? 'registrar la llegada a planta'
          : servicio.estado === 'PROGRAMADO'
            ? 'anular la programación'
            : 'cancelar el viaje';

    const confirmado = await this.dialogos.confirmar(
      `¿Deseas ${accion} del servicio ` + `${servicio.codigo}?`,
    );

    if (!confirmado) {
      return;
    }

    this.procesandoId.set(servicio.id);
    this.mensajeAccion.set('');
    this.mensajeExito.set('');
    this.mensajeError.set('');

    this.servicioApi.cambiarEstado(servicio.id, nuevoEstado).subscribe({
      next: (actualizado) => {
        this.servicios.update((lista) =>
          lista.map((item) => (item.id === actualizado.id ? actualizado : item)),
        );

        this.procesandoId.set(null);

        this.mensajeAccion.set(`El servicio ${servicio.codigo} ` + 'se actualizó correctamente.');
      },
      error: (error) => {
        this.procesandoId.set(null);

        this.mensajeAccion.set(
          error.error?.mensaje ||
            error.error?.message ||
            `No fue posible ${accion} ` + 'del servicio.',
        );
      },
    });
  }

  abrirEncuesta(servicio: ServicioTransporte): void {
    if (servicio.estado !== 'COMPLETADO') {
      return;
    }

    this.servicioEncuesta.set(servicio);
    this.encuestaActual.set(null);
    this.mensajeEncuesta.set('');
    this.mostrarEncuesta.set(true);
    this.limpiarFormularioEncuesta();
    this.cargandoEncuesta.set(true);

    this.encuestaApi.buscarPorServicio(servicio.id).subscribe({
      next: (encuesta) => {
        this.encuestaActual.set(encuesta);
        this.cargandoEncuesta.set(false);
      },
      error: (error) => {
        this.cargandoEncuesta.set(false);

        /*
         * Un 404 significa que el servicio todavía
         * puede ser evaluado. No se muestra como error.
         */
        if (error.status === 404) {
          return;
        }

        if (error.status === 401) {
          this.sesionService.cerrarSesionLocal();
          return;
        }

        this.mensajeEncuesta.set(
          error.error?.mensaje || error.error?.message || 'No fue posible consultar la evaluación.',
        );
      },
    });
  }

  cerrarEncuesta(): void {
    if (this.guardandoEncuesta()) {
      return;
    }

    this.mostrarEncuesta.set(false);
    this.servicioEncuesta.set(null);
    this.encuestaActual.set(null);
    this.mensajeEncuesta.set('');
    this.limpiarFormularioEncuesta();
  }

  registrarEncuesta(): void {
    const servicio = this.servicioEncuesta();

    if (!servicio || servicio.estado !== 'COMPLETADO' || this.encuestaActual()) {
      return;
    }

    this.mensajeEncuesta.set('');

    if (this.formularioEncuesta.invalid) {
      this.formularioEncuesta.markAllAsTouched();

      this.mensajeEncuesta.set(
        'Selecciona una calificación del 1 al 5 ' + 'en todos los criterios.',
      );
      return;
    }

    const datos = this.formularioEncuesta.getRawValue();

    this.guardandoEncuesta.set(true);

    this.encuestaApi
      .registrar(servicio.id, {
        puntualidad: datos.puntualidad,
        cumplimiento: datos.cumplimiento,
        atencion: datos.atencion,
        satisfaccionGeneral: datos.satisfaccionGeneral,
        comentarios: datos.comentarios.trim(),
      })
      .subscribe({
        next: (encuesta) => {
          this.encuestaActual.set(encuesta);
          this.guardandoEncuesta.set(false);

          this.mensajeEncuesta.set('La evaluación se registró correctamente.');
        },
        error: (error) => {
          this.guardandoEncuesta.set(false);

          if (error.status === 401) {
            this.sesionService.cerrarSesionLocal();
            return;
          }

          this.mensajeEncuesta.set(
            error.error?.mensaje ||
              error.error?.message ||
              'No fue posible registrar la evaluación.',
          );
        },
      });
  }

  textoCalificacion(valor: number): string {
    const etiquetas: Record<number, string> = {
      1: 'Muy deficiente',
      2: 'Deficiente',
      3: 'Regular',
      4: 'Bueno',
      5: 'Excelente',
    };

    return etiquetas[valor] || 'Sin calificación';
  }

  abrirEvidencias(servicio: ServicioTransporte): void {
    this.servicioSeleccionado.set(servicio);
    this.mostrarEvidencias.set(true);
    this.mensajeEvidencia.set('');
    this.evidencias.set([]);

    this.limpiarFormularioEvidencia();
    this.cargarEvidencias(servicio.id);
  }

  cerrarEvidencias(): void {
    if (this.guardandoEvidencia()) {
      return;
    }

    this.mostrarEvidencias.set(false);
    this.servicioSeleccionado.set(null);
    this.evidencias.set([]);
    this.mensajeEvidencia.set('');

    this.limpiarFormularioEvidencia();
  }

  cargarEvidencias(servicioId: number): void {
    this.cargandoEvidencias.set(true);
    this.mensajeEvidencia.set('');

    this.evidenciaApi.listarPorServicio(servicioId).subscribe({
      next: (evidencias) => {
        this.evidencias.set(evidencias);
        this.cargandoEvidencias.set(false);
      },
      error: (error) => {
        this.cargandoEvidencias.set(false);

        if (error.status === 401) {
          this.sesionService.cerrarSesionLocal();
          return;
        }

        this.mensajeEvidencia.set(
          error.error?.mensaje || error.error?.message || 'No fue posible cargar las evidencias.',
        );
      },
    });
  }

  seleccionarArchivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;

    const archivo = input.files?.item(0) ?? null;

    this.mensajeEvidencia.set('');

    if (!archivo) {
      this.archivoSeleccionado.set(null);
      this.nombreArchivo.set('');
      return;
    }

    const tiposPermitidos = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

    if (!tiposPermitidos.includes(archivo.type)) {
      this.archivoSeleccionado.set(null);
      this.nombreArchivo.set('');
      input.value = '';

      this.mensajeEvidencia.set('Solo se permiten imágenes JPG, PNG, WEBP o PDF.');
      return;
    }

    const limite = 10 * 1024 * 1024;

    if (archivo.size > limite) {
      this.archivoSeleccionado.set(null);
      this.nombreArchivo.set('');
      input.value = '';

      this.mensajeEvidencia.set('El archivo no debe superar los 10 MB.');
      return;
    }

    this.archivoSeleccionado.set(archivo);
    this.nombreArchivo.set(archivo.name);
  }

  obtenerUbicacionActual(): void {
    this.mensajeEvidencia.set('');

    if (!navigator.geolocation) {
      this.mensajeEvidencia.set('Este dispositivo no permite obtener la ubicación.');
      return;
    }

    this.obteniendoUbicacion.set(true);

    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        this.formularioEvidencia.patchValue({
          latitud: Number(posicion.coords.latitude.toFixed(7)),
          longitud: Number(posicion.coords.longitude.toFixed(7)),
        });

        this.obteniendoUbicacion.set(false);
      },
      () => {
        this.obteniendoUbicacion.set(false);

        this.mensajeEvidencia.set(
          'No fue posible obtener la ubicación. ' + 'Verifica el permiso del navegador.',
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 15_000,
        maximumAge: 30_000,
      },
    );
  }

  quitarUbicacion(): void {
    this.formularioEvidencia.patchValue({
      latitud: null,
      longitud: null,
    });
  }

  registrarEvidencia(): void {
    const servicio = this.servicioSeleccionado();

    const archivo = this.archivoSeleccionado();

    if (!servicio) {
      return;
    }

    if (servicio.estado !== 'EN_CURSO') {
      this.mensajeEvidencia.set(
        'Registro cerrado. Solo se admiten evidencias mientras el viaje está en curso.',
      );
      return;
    }

    this.mensajeEvidencia.set('');

    if (this.formularioEvidencia.invalid) {
      this.formularioEvidencia.markAllAsTouched();

      this.mensajeEvidencia.set('Completa los campos obligatorios.');
      return;
    }

    if (!archivo) {
      this.mensajeEvidencia.set('Selecciona una fotografía o documento.');
      return;
    }

    const datos = this.formularioEvidencia.getRawValue();

    if (
      datos.tipo === 'CONFORMIDAD' &&
      (!datos.nombreResponsable.trim() || !datos.documentoResponsable.trim())
    ) {
      this.mensajeEvidencia.set(
        'La conformidad requiere el nombre ' + 'y documento del responsable.',
      );
      return;
    }

    this.guardandoEvidencia.set(true);

    this.evidenciaApi
      .registrar(servicio.id, {
        tipo: datos.tipo,
        archivo,
        descripcion: datos.descripcion.trim() || null,
        latitud: datos.latitud,
        longitud: datos.longitud,
        nombreResponsable: datos.nombreResponsable.trim() || null,
        documentoResponsable: datos.documentoResponsable.trim() || null,
      })
      .subscribe({
        next: (evidencia) => {
          this.evidencias.update((lista) => [evidencia, ...lista]);

          this.guardandoEvidencia.set(false);

          this.limpiarFormularioEvidencia();

          this.mensajeEvidencia.set('La evidencia se registró correctamente.');
        },
        error: (error) => {
          this.guardandoEvidencia.set(false);

          if (error.status === 401) {
            this.sesionService.cerrarSesionLocal();
            return;
          }

          this.mensajeEvidencia.set(
            error.error?.mensaje ||
              error.error?.message ||
              'No fue posible registrar la evidencia.',
          );
        },
      });
  }

  abrirArchivo(evidencia: EvidenciaServicio): void {
    this.mensajeEvidencia.set('');
    abrirVistaPreviaEvidencia(
      evidencia,
      () => this.evidenciaApi.descargar(evidencia.id),
      (mensaje) => this.mensajeEvidencia.set(mensaje),
    );
  }

  mostrarCamposConformidad(): boolean {
    return this.formularioEvidencia.controls.tipo.value === 'CONFORMIDAD';
  }

  etiquetaTipoEvidencia(tipo: TipoEvidencia): string {
    const etiquetas: Record<TipoEvidencia, string> = {
      FOTO_AVERIA: 'Fotografía de avería',
      FOTO_INICIO: 'Fotografía de inicio',
      FOTO_CARGA: 'Fotografía de carga',
      FOTO_ENTREGA: 'Fotografía de entrega',
      DOCUMENTO: 'Documento',
      CONFORMIDAD: 'Conformidad del servicio',
    };

    return etiquetas[tipo];
  }

  formatearTamano(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatearFecha(fecha: string): string {
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(fecha));
  }

  calcularDuracion(servicio: ServicioTransporte): string {
    if (
      !servicio.fechaHoraSalidaReal ||
      !servicio.fechaHoraLlegadaReal ||
      servicio.estado !== 'COMPLETADO'
    ) {
      return '—';
    }

    const salida = new Date(servicio.fechaHoraSalidaReal).getTime();

    const llegada = new Date(servicio.fechaHoraLlegadaReal).getTime();

    const diferencia = llegada - salida;

    if (Number.isNaN(salida) || Number.isNaN(llegada) || diferencia < 0) {
      return '—';
    }

    const minutosTotales = Math.floor(diferencia / 60000);

    if (minutosTotales < 1) {
      return 'Menos de 1 min';
    }

    if (minutosTotales < 60) {
      return `${minutosTotales} min`;
    }

    const horas = Math.floor(minutosTotales / 60);

    const minutos = minutosTotales % 60;

    if (minutos === 0) {
      return `${horas} h`;
    }

    return `${horas} h ${minutos} min`;
  }

  irSolicitudes(): void {
    this.router.navigate(['/solicitudes']);
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private limpiarFormularioEvidencia(): void {
    this.formularioEvidencia.reset({
      tipo: 'FOTO_INICIO',
      descripcion: '',
      latitud: null,
      longitud: null,
      nombreResponsable: '',
      documentoResponsable: '',
    });

    this.archivoSeleccionado.set(null);
    this.nombreArchivo.set('');
  }

  private limpiarFormularioEncuesta(): void {
    this.formularioEncuesta.reset({
      puntualidad: 5,
      cumplimiento: 5,
      atencion: 5,
      satisfaccionGeneral: 5,
      comentarios: '',
    });
  }
}

interface ValidacionOperativa {
  id: number;
  servicioId: number;
  estado: 'VALIDADO' | 'OBSERVADO';
  comentario: string | null;
  usuario: string;
  responsable: string;
  fecha: string;
}
