import { nombreProducto } from '../../services/presentacion-viaje';
import { HistorialViajesComponent } from './historial-viajes';
import { validarLecturaViaje } from '../../services/validacion-odometro';
import { abrirVistaPreviaEvidencia } from '../../services/vista-previa-evidencia';
import { IncidenciaViajeComponent } from '../../components/incidencia-viaje';
import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import {
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';

import { ChoferService } from '../../services/chofer';
import { ViajesDispositivoService } from '../../services/viajes-dispositivo.service';
import { AccesoOfflineService } from '../../services/acceso-offline.service';
import { FotosOfflineService } from '../../services/fotos-offline.service';
import { AvanceViajeComponent, tramosConPendientes } from '../../components/avance-viaje';
import { EstadoServicio, ServicioTransporte } from '../../services/servicio-transporte.service';

import { SesionService } from '../../services/sesion.service';
import {
  MarcacionPendiente,
  MarcacionServicioResponse,
  MarcacionServicioService,
  TipoMarcacionServicio,
} from '../../services/marcacion-servicio.service';

type TipoEvidencia =
  'FOTO_AVERIA' | 'FOTO_INICIO' | 'FOTO_CARGA' | 'FOTO_ENTREGA' | 'DOCUMENTO' | 'CONFORMIDAD';

interface EvidenciaServicio {
  id: number;
  tipo: TipoEvidencia;
  nombreOriginal: string;
  tipoContenido: string;
  tamanoBytes: number;
  descripcion: string | null;
  latitud: number | null;
  longitud: number | null;
  nombreResponsable: string | null;
  documentoResponsable: string | null;
  registradoPor: string;
  fechaRegistro: string;
}

@Component({
  selector: 'app-chofer',
  imports: [
    HistorialViajesComponent,
    IncidenciaViajeComponent,
    ReactiveFormsModule,
    AvanceViajeComponent,
    DatePipe,
  ],
  templateUrl: './chofer.html',
  styleUrl: './chofer.scss',
})
export class Chofer implements OnInit, OnDestroy {
  private readonly dialogos = inyectarDialogos(DialogosService);
  private readonly choferService = inject(ChoferService);
  private readonly viajesDispositivo = inject(ViajesDispositivoService);
  private readonly accesoLocal = inject(AccesoOfflineService);
  private readonly fotosOffline = inject(FotosOfflineService);
  readonly fraseLocal = signal('');
  readonly mensajeAccesoLocal = signal('');
  readonly fotosPendientes = signal(0);
  readonly preparandoAcceso = signal(false);
  readonly preparandoTelefono = signal(false);
  readonly resultadoPreparacion = signal('');
  async prepararTelefono(): Promise<void> {
    if (this.preparandoTelefono() || this.cargando() || this.preparandoAcceso()) return;
    this.preparandoTelefono.set(true);
    this.resultadoPreparacion.set('');
    try {
      if (this.esAccesoLocal) {
        this.accesoLocal.validarSesionLocal();
        this.resultadoPreparacion.set(
          'Tu viaje guardado sigue disponible. Inicia sesión con acceso a la red corporativa para actualizarlo.',
        );
        return;
      }
      const preparado = await this.cargarServicios();
      if (!preparado) {
        this.resultadoPreparacion.set(
          this.necesitaMigracion()
            ? 'Hay archivos de una versión anterior. Completa su migración una sola vez en Revisar problema.'
            : 'No se pudo completar la preparación. Conserva tus datos y vuelve a intentarlo en la red corporativa.',
        );
        return;
      }
      await navigator.storage?.persist?.().catch(() => false);
      if (!this.serviciosPendientes().length) {
        this.resultadoPreparacion.set('Teléfono preparado. Aún no tienes un viaje asignado.');
      } else if (!navigator.serviceWorker?.controller) {
        this.resultadoPreparacion.set(
          'Viaje y fotos preparados. Abre la aplicación instalada con conexión para terminar de habilitar el uso sin red.',
        );
      } else {
        this.resultadoPreparacion.set(
          'Viaje guardado y fotografías protegidas. No borres los datos de la aplicación durante el recorrido.',
        );
      }
    } catch (error) {
      this.resultadoPreparacion.set(
        error instanceof Error
          ? error.message
          : 'No se pudo preparar este teléfono. Tus pendientes se conservan.',
      );
    } finally {
      this.preparandoTelefono.set(false);
    }
  }
  readonly configuracionDispositivo = signal(false);
  readonly archivosDesbloqueados = signal(this.accesoLocal.desbloqueado());
  readonly necesitaMigracion = signal(this.accesoLocal.anteriorDisponible());
  readonly colaRevisada = signal(false);
  readonly esAccesoLocal = sessionStorage.getItem('accesoOffline') === 'true';
  async prepararAccesoLocal(): Promise<void> {
    if (this.preparandoAcceso()) return;
    this.preparandoAcceso.set(true);
    try {
      if (this.esAccesoLocal)
        throw new Error(
          'Conéctate e inicia sesión para actualizar la protección anterior. Tus pendientes se conservan.',
        );
      await this.accesoLocal.prepararAutomatico(this.fraseLocal() || undefined);
      this.archivosDesbloqueados.set(this.accesoLocal.desbloqueado());
      this.necesitaMigracion.set(
        this.accesoLocal.anteriorDisponible() && !this.accesoLocal.desbloqueado(),
      );
      const persistente = await navigator.storage?.persist?.().catch(() => false);
      this.mensajeAccesoLocal.set(
        'Dispositivo preparado. El sistema gestiona la protección de tus archivos; ya no necesitas una frase adicional.' +
          (persistente
            ? ' Almacenamiento persistente autorizado.'
            : ' El navegador no garantizó almacenamiento persistente; verifica espacio disponible y sincroniza al regresar.'),
      );
    } catch (e) {
      this.mensajeAccesoLocal.set(
        e instanceof Error ? e.message : 'No se pudo preparar el acceso local.',
      );
    } finally {
      this.fraseLocal.set('');
      this.preparandoAcceso.set(false);
    }
  }
  private async prepararDispositivo(): Promise<void> {
    this.preparandoAcceso.set(true);
    try {
      if (this.esAccesoLocal) {
        this.accesoLocal.validarSesionLocal();
        if (
          !this.accesoLocal.desbloqueado() &&
          sessionStorage.getItem('modoAccesoLocal') === 'automatico'
        )
          await this.accesoLocal.reanudarAutomatico();
      } else {
        await this.accesoLocal.prepararAutomatico();
      }
      await navigator.storage?.persist?.().catch(() => false);
      this.archivosDesbloqueados.set(this.accesoLocal.desbloqueado());
      this.necesitaMigracion.set(
        this.accesoLocal.anteriorDisponible() && !this.accesoLocal.desbloqueado(),
      );
    } catch (error) {
      this.mensajeAccesoLocal.set(
        error instanceof Error
          ? error.message
          : 'No se pudo preparar este dispositivo. Tus pendientes se conservan.',
      );
      if (this.necesitaMigracion()) this.configuracionDispositivo.set(true);
    } finally {
      this.preparandoAcceso.set(false);
    }
  }
  irAutenticacion(): void {
    void this.router.navigate(['/login']);
  }
  readonly fechaDescarga = signal<string | null>(null);
  readonly errorDescarga = signal('');
  private readonly http = inject(HttpClient);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly sesionService = inject(SesionService);
  private readonly marcacionService = inject(MarcacionServicioService);
  private temporizadorSincronizacion: number | null = null;
  readonly estadoPwa = signal('Comprobando preparación para uso sin conexión…');
  private readonly alCambiarControlador = () => this.comprobarPreparacionPwa();
  private readonly alVolverALaPantalla = () => {
    if (document.visibilityState === 'visible') {
      this.comprobarPreparacionPwa();
      void this.sincronizarMarcaciones(false);
    }
  };
  private comprobarPreparacionPwa(): void {
    if (!window.isSecureContext) {
      this.estadoPwa.set(
        'Se necesita HTTPS confiable para preparar el uso sin conexión, la cámara y el GPS.',
      );
    } else if (!('serviceWorker' in navigator)) {
      this.estadoPwa.set(
        'Este navegador no permite preparar la PWA. Usa un navegador compatible en un equipo autorizado.',
      );
    } else if (!navigator.serviceWorker.controller) {
      this.estadoPwa.set(
        'La PWA aún no controla esta pantalla. Completa la instalación y vuelve a abrirla en la red corporativa antes de salir.',
      );
    } else {
      this.estadoPwa.set(
        'Aplicación disponible para abrir sin conexión. Conserva los datos de este navegador durante el viaje.',
      );
    }
  }
  private readonly alRecuperarConexion = () => {
    void this.sincronizarMarcaciones();
  };

  readonly nombreChofer = signal(sessionStorage.getItem('nombres') || 'Chofer');
  readonly primerNombre = computed(() => this.nombreChofer().trim().split(/\s+/)[0] || 'chofer');
  readonly avisoViaje = signal<ServicioTransporte | null>(null);
  private readonly dialogoAsignacion = viewChild<ElementRef<HTMLDialogElement>>('asignacionDialog');
  private readonly avisosVistos = new Set<number>();
  private readonly controlarAviso = effect(() => {
    const elemento = this.dialogoAsignacion()?.nativeElement;
    if (!elemento) return;
    if (this.avisoViaje() && !elemento.open) elemento.showModal();
    else if (!this.avisoViaje() && elemento.open) elemento.close();
  });
  private avisarNuevaAsignacion(): void {
    if (this.mostrarHistorial() || this.avisoViaje()) return;
    const usuario = sessionStorage.getItem('usuarioId');
    const viaje = this.serviciosPendientes().find(
      (s) =>
        !s.tramos?.length ||
        !tramosConPendientes(s, this.marcacionesPendientes()).every((t) => !!t.llegadaReal),
    );
    if (viaje && this.avisosVistos.has(viaje.id)) return;
    try {
      if (viaje && sessionStorage.getItem(`transporte-aviso-${usuario}-${viaje.id}`)) return;
    } catch {
      /* Conservar al menos el estado de esta pantalla. */
    }
    if (viaje) this.avisoViaje.set(viaje);
  }
  cerrarAviso(verViaje = false): void {
    const viaje = this.avisoViaje();
    if (!viaje) return;
    this.avisosVistos.add(viaje.id);
    try {
      sessionStorage.setItem(
        `transporte-aviso-${sessionStorage.getItem('usuarioId')}-${viaje.id}`,
        'visto',
      );
    } catch {
      /* Conservar al menos el aviso de esta pantalla. */
    }
    this.avisoViaje.set(null);
    this.dialogoAsignacion()?.nativeElement.close();
    if (verViaje) {
      const tarjeta = document.getElementById('viaje-' + viaje.id);
      tarjeta?.scrollIntoView({ block: 'center', behavior: 'auto' });
      tarjeta?.querySelector<HTMLButtonElement>('.primary-button')?.focus({ preventScroll: true });
    }
  }
  readonly mostrarHistorial = signal(false);

  readonly servicios = signal<ServicioTransporte[]>([]);
  readonly cargando = signal(true);
  readonly procesandoId = signal<number | null>(null);
  readonly mensajeError = signal('');
  readonly mensajeExito = signal('');
  readonly marcacionesPendientes = signal<MarcacionPendiente[]>([]);
  readonly marcacionesServidor = signal<Record<number, MarcacionServicioResponse[]>>({});
  readonly sincronizando = signal(false);
  readonly usandoCopiaLocal = signal(false);

  readonly mostrarEvidencias = signal(false);
  readonly servicioSeleccionado = signal<ServicioTransporte | null>(null);
  readonly evidencias = signal<EvidenciaServicio[]>([]);
  readonly cargandoEvidencias = signal(false);
  readonly guardandoEvidencia = signal(false);
  readonly obteniendoUbicacion = signal(false);
  readonly mensajeEvidencia = signal('');
  readonly archivoSeleccionado = signal<File | null>(null);
  readonly nombreArchivo = signal('');

  readonly formularioEvidencia = this.formBuilder.group({
    tipo: this.formBuilder.nonNullable.control<TipoEvidencia>('FOTO_INICIO', Validators.required),
    descripcion: this.formBuilder.nonNullable.control('', [Validators.maxLength(500)]),
    latitud: this.formBuilder.control<number | null>(null),
    longitud: this.formBuilder.control<number | null>(null),
    nombreResponsable: this.formBuilder.nonNullable.control('', [Validators.maxLength(150)]),
    documentoResponsable: this.formBuilder.nonNullable.control('', [Validators.maxLength(30)]),
  });

  readonly serviciosPendientes = computed(() =>
    this.servicios()
      .filter((servicio) => servicio.estado === 'PROGRAMADO' || servicio.estado === 'EN_CURSO')
      .sort(
        (a, b) =>
          Number(this.viajeEnCursoLocal(b)) - Number(this.viajeEnCursoLocal(a)) ||
          (a.fechaHoraSalida || '').localeCompare(b.fechaHoraSalida || '') ||
          a.id - b.id,
      ),
  );

  readonly serviciosFinalizados = computed(() =>
    this.servicios().filter(
      (servicio) =>
        servicio.estado === 'COMPLETADO' ||
        servicio.estado === 'CANCELADO' ||
        servicio.estado === 'INTERRUMPIDO',
    ),
  );

  readonly puedeRegistrarEvidencia = computed(
    () => !!this.servicioSeleccionado() && this.viajeEnCursoLocal(this.servicioSeleccionado()!),
  );
  private viajeEnCursoLocal(servicio: ServicioTransporte): boolean {
    const tramos = tramosConPendientes(servicio, this.marcacionesPendientes());
    if (tramos.length)
      return (
        tramos.some((t) => !!t.salidaReal) &&
        !tramos.every((t) => !!t.llegadaReal) &&
        servicio.estado !== 'CANCELADO' &&
        servicio.estado !== 'INTERRUMPIDO'
      );
    return servicio.estado === 'EN_CURSO';
  }

  ngOnInit(): void {
    this.mostrarHistorial.set(this.router.url.startsWith('/chofer/historial'));
    this.validarAcceso();
    if (this.esAccesoLocal) void this.prepararDispositivo().then(() => this.cargarServicios());
    else this.cargarServicios();
    window.addEventListener('online', this.alRecuperarConexion);
    document.addEventListener('visibilitychange', this.alVolverALaPantalla);
    navigator.serviceWorker?.addEventListener('controllerchange', this.alCambiarControlador);
    this.comprobarPreparacionPwa();
    this.temporizadorSincronizacion = window.setInterval(
      () => void this.sincronizarMarcaciones(false),
      30_000,
    );
    void this.sincronizarMarcaciones(false);
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.alRecuperarConexion);
    document.removeEventListener('visibilitychange', this.alVolverALaPantalla);
    navigator.serviceWorker?.removeEventListener('controllerchange', this.alCambiarControlador);
    if (this.temporizadorSincronizacion !== null) {
      window.clearInterval(this.temporizadorSincronizacion);
    }
  }

  private validarAcceso(): void {
    if (sessionStorage.getItem('rol') !== 'CHOFER') {
      this.router.navigate(['/dashboard']);
    }
  }

  cargarServicios(): Promise<boolean> {
    this.cargando.set(true);
    this.mensajeError.set('');

    return new Promise((resolve) =>
      this.choferService.listarServicios().subscribe({
        next: (servicios) => {
          this.usandoCopiaLocal.set(false);
          try {
            this.fechaDescarga.set(this.viajesDispositivo.guardar(servicios));
            this.errorDescarga.set('');
          } catch {
            this.errorDescarga.set(
              'No se pudo guardar el itinerario en este dispositivo. No salgas de planta hasta resolver el almacenamiento.',
            );
          }
          this.servicios.set([...servicios].sort((primero, segundo) => segundo.id - primero.id));
          void this.actualizarColaLocal();
          this.cargarMarcacionesServidor(servicios);
          this.cargando.set(false);
          this.avisarNuevaAsignacion();
          // Solo renovar preparación automática después de que el servidor acepte la sesión.
          void this.prepararDispositivo()
            .then(async () => {
              await this.actualizarColaLocal();
              resolve(!this.errorDescarga() && this.archivosDesbloqueados());
              void this.sincronizarMarcaciones(false);
            })
            .catch(() => resolve(false));
        },
        error: (error: HttpErrorResponse) => {
          this.cargando.set(false);

          if (error.status === 0 || error.status >= 500) {
            try {
              const copia = this.viajesDispositivo.leer();
              if (copia) {
                this.servicios.set(copia.servicios);
                this.fechaDescarga.set(copia.fecha);
                this.usandoCopiaLocal.set(true);
                void this.actualizarColaLocal();
                resolve(false);
                return;
              }
            } catch {
              /* Mostrar el error si no existe una copia válida. */
            }
          }

          if (error.status === 401 || error.status === 403) {
            this.sesionService.cerrarSesionLocal();
            resolve(false);
            return;
          }

          this.mensajeError.set(
            this.obtenerMensajeError(error, 'No fue posible cargar tus servicios asignados.'),
          );
          resolve(false);
        },
      }),
    );
  }

  actualizarIncidencia(actualizado: ServicioTransporte) {
    this.servicios.update((lista) => lista.map((s) => (s.id === actualizado.id ? actualizado : s)));
    try {
      this.viajesDispositivo.guardar(this.servicios());
    } catch {}
    void this.cargarServicios();
  }
  lecturasOdometro: Record<number, number | null> = {};
  sinLecturaOdometro: Partial<Record<number, boolean>> = {};
  motivosOdometro: Partial<Record<number, string>> = {};
  referenciaOdometro(s: ServicioTransporte): { km: number | null; averiado: boolean } {
    let km = s.vehiculo.odometroKm ?? null,
      averiado = s.vehiculo.estadoOdometro === 'AVERIADO';
    for (const e of this.marcacionesPendientes()
      .filter((e) =>
        this.servicios().some((v) => v.id === e.servicioId && v.vehiculo.id === s.vehiculo.id),
      )
      .sort((a, b) => a.fechaHoraDispositivo.localeCompare(b.fechaHoraDispositivo))) {
      if (e.odometroKm != null) km = Math.max(km ?? 0, e.odometroKm);
      if (e.odometroNoDisponible) averiado = true;
    }
    return { km, averiado };
  }
  sinOdometro(s: ServicioTransporte): boolean {
    return this.sinLecturaOdometro[s.id] ?? this.referenciaOdometro(s).averiado;
  }

  requiereOdometro(servicio: ServicioTransporte): boolean {
    const t = this.tramoActual(servicio);
    const tipo = this.siguienteMarcacion(servicio);
    return (
      !!t &&
      ((t.orden === 1 && tipo === 'SALIDA_IDA') ||
        (t.orden === servicio.tramos?.length && tipo === 'LLEGADA_DESTINO'))
    );
  }
  lecturaSalida(servicio: ServicioTransporte): number | null {
    return tramosConPendientes(servicio, this.marcacionesPendientes())[0]?.odometroSalidaKm ?? null;
  }
  cambiarOdometro(id: number, evento: Event): void {
    const v = (evento.target as HTMLInputElement).value;
    this.lecturasOdometro[id] = v === '' ? null : Number(v);
  }
  async registrarSiguienteMarcacion(servicio: ServicioTransporte): Promise<void> {
    const tipo = this.siguienteMarcacion(servicio);

    if (!tipo || this.procesandoId() !== null) {
      return;
    }

    const requiere = this.requiereOdometro(servicio),
      sinLectura = requiere && this.sinOdometro(servicio);
    const lectura = requiere && !sinLectura ? (this.lecturasOdometro[servicio.id] ?? null) : null;
    const motivo = sinLectura ? (this.motivosOdometro[servicio.id] ?? '').trim() : null;
    if (requiere) {
      const ref = this.referenciaOdometro(servicio);
      const actual = {
        ...servicio,
        tramos: tramosConPendientes(servicio, this.marcacionesPendientes()),
      };
      const error = validarLecturaViaje(
        actual,
        tipo === 'SALIDA_IDA',
        lectura,
        sinLectura,
        motivo ?? '',
        ref.km,
        ref.averiado,
      );
      if (error) {
        this.mensajeError.set(error);
        return;
      }
    }
    if (!(await this.dialogos.confirmar(`¿Confirmas "${this.etiquetaAccion(servicio)}"?`))) {
      return;
    }

    this.procesandoId.set(servicio.id);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    try {
      const capturada = await this.marcacionService.capturar(
        servicio.id,
        tipo,
        null,
        this.tramoActual(servicio)?.orden ?? null,
        lectura,
        sinLectura,
        motivo,
      );
      delete this.lecturasOdometro[servicio.id];
      delete this.sinLecturaOdometro[servicio.id];
      delete this.motivosOdometro[servicio.id];
      await this.actualizarColaLocal();
      const resultado = await this.sincronizarMarcaciones(false);

      if (resultado && resultado.enviados > 0) {
        this.mensajeExito.set(
          `${this.etiquetaMarcacion(tipo)} registrada y sincronizada. ` +
            `GPS ±${Math.round(capturada.precisionMetros)} m.`,
        );
      } else {
        this.mensajeExito.set(
          `${this.etiquetaMarcacion(tipo)} guardada en el celular. ` +
            'Se enviará al abrir la aplicación con acceso a la red corporativa.',
        );
      }
      if (tipo === 'SALIDA_IDA') {
        const actualizado = this.servicios().find((item) => item.id === servicio.id) ?? servicio;
        this.abrirEvidencias(actualizado);
        this.formularioEvidencia.controls.tipo.setValue('FOTO_INICIO');
      }
    } catch (error: any) {
      this.mensajeError.set(error?.message || 'No fue posible registrar la ubicación.');
    } finally {
      this.procesandoId.set(null);
    }
  }

  async cancelarServicio(servicio: ServicioTransporte): Promise<void> {
    if (servicio.estado !== 'EN_CURSO') {
      return;
    }

    if (
      await this.dialogos.confirmar(`¿Confirmas la cancelación del servicio ${servicio.codigo}?`)
    ) {
      this.actualizarEstado(servicio, 'CANCELADO');
    }
  }

  private actualizarEstado(servicio: ServicioTransporte, estado: EstadoServicio): void {
    if (this.procesandoId() !== null) {
      return;
    }

    this.procesandoId.set(servicio.id);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.choferService.cambiarEstado(servicio.id, estado).subscribe({
      next: (actualizado) => {
        this.servicios.update((lista) =>
          lista.map((item) => (item.id === actualizado.id ? actualizado : item)),
        );
        this.procesandoId.set(null);

        const mensaje =
          estado === 'EN_CURSO'
            ? `Se registró la salida del servicio ${servicio.codigo}.`
            : estado === 'COMPLETADO'
              ? `Se registró la llegada del servicio ${servicio.codigo}.`
              : `El servicio ${servicio.codigo} fue cancelado.`;

        this.mensajeExito.set(mensaje);
      },
      error: (error: HttpErrorResponse) => {
        this.procesandoId.set(null);
        this.mensajeError.set(
          this.obtenerMensajeError(error, 'No fue posible actualizar el servicio.'),
        );
      },
    });
  }

  siguienteMarcacion(servicio: ServicioTransporte): TipoMarcacionServicio | null {
    if (servicio.detenidoPorAveria || servicio.estado === 'INTERRUMPIDO') return null;
    if (servicio.tramos?.length) {
      const tramo = this.tramoActual(servicio);
      return tramo ? (tramo.salidaReal ? 'LLEGADA_DESTINO' : 'SALIDA_IDA') : null;
    }
    const tipos = new Set<TipoMarcacionServicio>([
      ...(this.marcacionesServidor()[servicio.id] || []).map((item) => item.tipo),
      ...this.marcacionesPendientes()
        .filter((item) => item.servicioId === servicio.id)
        .map((item) => item.tipo),
    ]);

    if (!tipos.has('SALIDA_IDA')) return 'SALIDA_IDA';
    if (!tipos.has('LLEGADA_DESTINO')) return 'LLEGADA_DESTINO';

    if (servicio.requiereRetorno && !tipos.has('SALIDA_RETORNO')) {
      return 'SALIDA_RETORNO';
    }

    if (servicio.requiereRetorno && !tipos.has('LLEGADA_BASE')) {
      return 'LLEGADA_BASE';
    }

    return null;
  }

  tramoActual(servicio: ServicioTransporte) {
    return tramosConPendientes(servicio, this.marcacionesPendientes()).find((t) => !t.llegadaReal);
  }
  etiquetaAccion(servicio: ServicioTransporte): string {
    const tramo = this.tramoActual(servicio);
    if (tramo) {
      if (servicio.servicioAuxiliadoId) {
        return tramo.orden === 1
          ? tramo.salidaReal
            ? 'Unidad recogida, regresar a planta'
            : 'Salir al auxilio'
          : 'Llegamos a planta';
      }
      if (tramo.actividad === 'RETORNO')
        return tramo.salidaReal
          ? `Finalizar viaje en ${tramo.destino}`
          : 'Iniciar regreso a planta';
      if (tramo.salidaReal) return `Llegué a ${tramo.destino}`;
      return tramo.orden === 1 ? 'Iniciar viaje' : `Continuar hacia ${tramo.destino}`;
    }
    const tipo = this.siguienteMarcacion(servicio);
    return tipo ? this.etiquetaMarcacion(tipo) : 'Viaje finalizado';
  }
  destinoActual(servicio: ServicioTransporte): string {
    return this.tramoActual(servicio)?.destino || servicio.destinoDescripcion;
  }
  estadoViajeVisible(servicio: ServicioTransporte): string {
    if (servicio.detenidoPorAveria) return 'DETENIDO POR AVERÍA';
    if (servicio.estado === 'INTERRUMPIDO') return 'INTERRUMPIDO POR AVERÍA';
    const tramos = tramosConPendientes(servicio, this.marcacionesPendientes());
    if (tramos.length && tramos.every((t) => !!t.llegadaReal)) return 'RECORRIDO FINALIZADO';
    return this.viajeEnCursoLocal(servicio) ? 'EN CURSO' : 'PROGRAMADO';
  }
  estadoPaso(servicio: ServicioTransporte): string {
    const averia = servicio.incidencias?.find(
      (i) => !['RESUELTA', 'RETORNO_CONFIRMADO'].includes(i.estado),
    );
    if (averia || servicio.detenidoPorAveria)
      return averia?.estado === 'REPORTADA'
        ? 'Avería reportada · esperando atención'
        : 'Detenido por avería';
    if (servicio.estado === 'INTERRUMPIDO') return 'Viaje interrumpido por avería';
    const tramo = this.tramoActual(servicio);
    if (!tramo && servicio.tramos?.length) return 'Espera la confirmación de envío del viaje.';
    if (!tramo) return servicio.estado === 'PROGRAMADO' ? 'Pendiente de salida' : 'Viaje en curso';
    if (tramo.salidaReal)
      return tramo.actividad === 'RETORNO' ? 'De regreso a planta' : 'En camino';
    return tramo.orden === 1 ? 'Pendiente de salida' : `En ${tramo.origen}`;
  }
  etiquetaMarcacion(tipo: TipoMarcacionServicio): string {
    return {
      SALIDA_IDA: 'Iniciar viaje de ida',
      LLEGADA_DESTINO: 'Registrar llegada al destino',
      SALIDA_RETORNO: 'Iniciar viaje de retorno',
      LLEGADA_BASE: 'Registrar llegada a base',
    }[tipo];
  }

  tienePendiente(servicioId: number): boolean {
    return this.marcacionesPendientes().some((item) => item.servicioId === servicioId);
  }

  async sincronizarMarcaciones(
    mostrarMensaje = true,
  ): Promise<{ enviados: number; pendientes: number } | null> {
    if (
      this.sincronizando() ||
      this.preparandoAcceso() ||
      !sessionStorage.getItem('usuarioId') ||
      this.esAccesoLocal
    )
      return null;

    this.sincronizando.set(true);
    try {
      const resultado = await this.marcacionService.sincronizarPendientes(
        this.servicios().map((s) => s.id),
      );
      for (const servicio of this.servicios().filter((s) => s.estado === 'EN_CURSO')) {
        await this.fotosOffline.sincronizar(servicio.id);
      }
      await this.actualizarColaLocal();

      if (resultado.enviados > 0) this.cargarServicios();

      if (mostrarMensaje && resultado.enviados > 0) {
        this.mensajeExito.set(`${resultado.enviados} marcación(es) sincronizada(s) correctamente.`);
      }
      return resultado;
    } catch {
      if (mostrarMensaje)
        this.mensajeError.set(
          'No se pudo sincronizar. Conserva los datos de este dispositivo y vuelve a conectarte a la red corporativa.',
        );
      return null;
    } finally {
      this.sincronizando.set(false);
    }
  }

  private async actualizarColaLocal(): Promise<void> {
    if (!sessionStorage.getItem('usuarioId')) return;
    this.fotosPendientes.set((await this.fotosOffline.pendientes()).length);
    this.marcacionesPendientes.set(
      (await this.marcacionService.pendientes()).filter((m) =>
        this.servicios().some((s) => s.id === m.servicioId),
      ),
    );
    this.colaRevisada.set(true);
  }

  private cargarMarcacionesServidor(servicios: ServicioTransporte[]): void {
    for (const servicio of servicios.filter(
      (item) => item.estado === 'PROGRAMADO' || item.estado === 'EN_CURSO',
    )) {
      this.marcacionService.listar(servicio.id).subscribe({
        next: (marcaciones) =>
          this.marcacionesServidor.update((actual) => ({
            ...actual,
            [servicio.id]: marcaciones,
          })),
      });
    }
  }

  abrirEvidencias(servicio: ServicioTransporte): void {
    this.servicioSeleccionado.set(servicio);
    this.mostrarEvidencias.set(true);
    this.mensajeEvidencia.set('');
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

  private cargarEvidencias(servicioId: number): void {
    this.cargandoEvidencias.set(true);

    this.http.get<EvidenciaServicio[]>(`/api/evidencias/servicio/${servicioId}`).subscribe({
      next: (evidencias) => {
        this.evidencias.set([...evidencias].sort((a, b) => b.id - a.id));
        this.cargandoEvidencias.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargandoEvidencias.set(false);
        this.mensajeEvidencia.set(
          this.obtenerMensajeError(error, 'No fue posible cargar las evidencias.'),
        );
      },
    });
  }

  seleccionarArchivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;

    if (!archivo) {
      return;
    }

    const tiposPermitidos = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

    if (!tiposPermitidos.includes(archivo.type)) {
      this.mensajeEvidencia.set('Solo se permiten imágenes JPG, PNG, WEBP o archivos PDF.');
      input.value = '';
      return;
    }

    if (archivo.size > 10 * 1024 * 1024) {
      this.mensajeEvidencia.set('El archivo supera el tamaño máximo de 10 MB.');
      input.value = '';
      return;
    }

    this.archivoSeleccionado.set(archivo);
    this.nombreArchivo.set(archivo.name);
    this.mensajeEvidencia.set('');
  }

  obtenerUbicacionActual(): void {
    if (!navigator.geolocation) {
      this.mensajeEvidencia.set('Este dispositivo no permite obtener la ubicación GPS.');
      return;
    }

    this.obteniendoUbicacion.set(true);
    this.mensajeEvidencia.set('');

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
          'No fue posible obtener la ubicación. Verifica el permiso del navegador.',
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0,
      },
    );
  }

  quitarUbicacion(): void {
    this.formularioEvidencia.patchValue({
      latitud: null,
      longitud: null,
    });
  }

  mostrarCamposConformidad(): boolean {
    return this.formularioEvidencia.controls.tipo.value === 'CONFORMIDAD';
  }

  async registrarEvidencia(): Promise<void> {
    const servicio = this.servicioSeleccionado();
    const archivo = this.archivoSeleccionado();
    const datos = this.formularioEvidencia.getRawValue();

    if (!servicio || !this.viajeEnCursoLocal(servicio)) {
      this.mensajeEvidencia.set(
        'Solo puedes registrar evidencias mientras el viaje está en proceso.',
      );
      return;
    }

    if (!archivo) {
      this.mensajeEvidencia.set('Selecciona una fotografía o documento.');
      return;
    }

    if (
      datos.tipo === 'CONFORMIDAD' &&
      (!datos.nombreResponsable.trim() || !datos.documentoResponsable.trim())
    ) {
      this.mensajeEvidencia.set('Ingresa el nombre y documento del responsable.');
      return;
    }

    const formulario = new FormData();
    formulario.append('archivo', archivo);
    formulario.append('tipo', datos.tipo);
    formulario.append('descripcion', datos.descripcion.trim());

    if (datos.latitud !== null) {
      formulario.append('latitud', String(datos.latitud));
    }

    if (datos.longitud !== null) {
      formulario.append('longitud', String(datos.longitud));
    }

    if (datos.tipo === 'CONFORMIDAD') {
      formulario.append('nombreResponsable', datos.nombreResponsable.trim());
      formulario.append('documentoResponsable', datos.documentoResponsable.trim());
    }

    this.guardandoEvidencia.set(true);
    this.mensajeEvidencia.set('');

    try {
      await this.fotosOffline.guardar(servicio.id, formulario);
      this.limpiarFormularioEvidencia();
      await this.actualizarColaLocal();
      this.mensajeEvidencia.set(
        'Archivo cifrado y guardado en este dispositivo. Pendiente de confirmación del servidor.',
      );
      await this.sincronizarMarcaciones(false);
      await this.actualizarColaLocal();
      if (!this.esAccesoLocal) this.cargarEvidencias(servicio.id);
    } catch (e) {
      this.mensajeEvidencia.set(
        e instanceof Error
          ? e.message
          : 'No se pudo guardar el archivo. Conserva la fotografía e inténtalo de nuevo.',
      );
    } finally {
      this.guardandoEvidencia.set(false);
    }
  }

  abrirArchivo(evidencia: EvidenciaServicio): void {
    this.mensajeEvidencia.set('');
    abrirVistaPreviaEvidencia(
      evidencia,
      () => this.http.get(`/api/evidencias/${evidencia.id}/archivo`, { responseType: 'blob' }),
      (mensaje) => this.mensajeEvidencia.set(mensaje),
    );
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

  formatearFecha(fecha: string): string {
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(fecha));
  }

  formatearTamano(bytes: number): string {
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  readonly nombreProducto = nombreProducto;
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

  private obtenerMensajeError(error: HttpErrorResponse, mensajePredeterminado: string): string {
    return error.error?.mensaje || error.error?.message || mensajePredeterminado;
  }
}
