import { AjustarFotoVehiculo } from '../../components/ajustar-foto-vehiculo';
import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, OnDestroy, DestroyRef, signal, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { switchMap, catchError, of } from 'rxjs';
import {
  EstadoVehiculo,
  MovimientoUnidad,
  SituacionUnidadRequest,
  MovimientoOdometro,
} from '../../services/vehiculo.service';
import { MantenimientoRequest } from '../../services/mantenimiento.service';

import { Vehiculo, VehiculoRequest, VehiculoService } from '../../services/vehiculo.service';

import { SesionService } from '../../services/sesion.service';
import { FotoVehiculoComponent } from '../../components/foto-vehiculo/foto-vehiculo';

@Component({
  selector: 'app-vehiculos',
  imports: [CommonModule, ReactiveFormsModule, FotoVehiculoComponent, AjustarFotoVehiculo],
  templateUrl: './vehiculos.html',
  styleUrl: './vehiculos.scss',
})
export class Vehiculos implements OnInit, OnDestroy {
  readonly borradorRestaurado = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogos = inyectarDialogos(DialogosService);

  vehiculos = signal<Vehiculo[]>([]);
  cargando = signal(true);
  guardando = signal(false);
  mostrarFormulario = signal(false);
  vehiculoEditandoId = signal<number | null>(null);
  mensajeError = signal('');
  mensajeExito = signal('');
  fotoSeleccionada = signal<File | null>(null);
  fotoOriginal = signal<File | null>(null);
  procesandoFoto = signal(false);
  fotoPreview = signal<string | null>(null);

  private readonly rutaActual = inject(ActivatedRoute, { optional: true });
  private enlaceProcesado = false;
  readonly filtroSituacion = signal('TODOS');
  readonly vehiculosFiltrados = computed(() =>
    this.vehiculos().filter(
      (v) =>
        this.filtroSituacion() === 'TODOS' ||
        (this.filtroSituacion() === 'OPERATIVOS'
          ? ['DISPONIBLE', 'EN_SERVICIO'].includes(v.estado)
          : v.estado === this.filtroSituacion()),
    ),
  );
  readonly seleccionandoBaja = signal(false);
  readonly bajaDesdeProceso = signal(false);
  abrirSeleccionBaja(): void {
    this.seleccionandoBaja.set(true);
  }
  seleccionarUnidadBaja(id: string): void {
    const unidad = this.vehiculos().find((v) => v.id === Number(id));
    if (unidad) {
      this.seleccionandoBaja.set(false);
      this.abrirGestionUnidad(unidad, 'BAJA');
      this.bajaDesdeProceso.set(true);
    }
  }
  readonly unidadGestion = signal<Vehiculo | null>(null);
  readonly movimientosUnidad = signal<MovimientoUnidad[]>([]);
  readonly leyendoHistorial = signal(false);
  readonly guardandoSituacion = signal(false);
  readonly errorSituacion = signal('');
  situacionForm;
  readonly hoySituacion = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  })();
  puedeBaja(): boolean {
    return ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(sessionStorage.getItem('rol') || '');
  }
  puedeGestionarUnidad(): boolean {
    return ['MASTER', 'COORDINADOR', 'ADMINISTRADOR', 'OPERADOR'].includes(
      sessionStorage.getItem('rol') || '',
    );
  }
  abrirGestionUnidad(
    unidad: Vehiculo,
    accion: 'SUSPENDER' | 'REHABILITAR' | 'BAJA' = 'BAJA',
  ): void {
    this.historialOdometroVisible.set(false);
    this.lecturasUnidad.set([]);
    this.bajaDesdeProceso.set(false);
    this.unidadGestion.set(unidad);
    this.movimientosUnidad.set([]);
    this.errorSituacion.set('');
    this.situacionForm.reset({
      accion,
      fechaEfectiva: this.hoySituacion,
      motivo: '',
      observaciones: '',
      referenciaSustento: '',
    });
    this.leyendoHistorial.set(true);
    this.vehiculoService.historialSituacion(unidad.id).subscribe({
      next: (datos) => {
        if (this.unidadGestion()?.id !== unidad.id) return;
        this.movimientosUnidad.set(datos);
        this.leyendoHistorial.set(false);
      },
      error: () => {
        if (this.unidadGestion()?.id !== unidad.id) return;
        this.leyendoHistorial.set(false);
        this.errorSituacion.set(
          'No se pudo consultar el historial. Cierra y vuelve a abrir la ficha.',
        );
      },
    });
  }
  cerrarGestionUnidad(): void {
    if (!this.guardandoSituacion()) this.unidadGestion.set(null);
  }
  async registrarSituacionUnidad(): Promise<void> {
    const unidad = this.unidadGestion();
    if (
      !unidad ||
      this.guardandoSituacion() ||
      this.leyendoHistorial() ||
      !this.puedeBaja() ||
      unidad.estado === 'BAJA_DEFINITIVA'
    )
      return;
    this.situacionForm.markAllAsTouched();
    const datos = this.situacionForm.getRawValue();
    if (this.situacionForm.invalid || !datos.motivo.trim()) {
      this.errorSituacion.set('Completa una fecha no futura y el motivo del movimiento.');
      return;
    }
    if (datos.fechaEfectiva > this.hoySituacion) {
      this.errorSituacion.set('La fecha efectiva no puede ser futura.');
      return;
    }
    if (datos.accion === 'BAJA' && !this.puedeBaja()) return;
    const aviso = `¿Estás seguro de dar de baja definitivamente la unidad ${unidad.placa} · ${unidad.marca} ${unidad.modelo}? Motivo: ${datos.motivo.trim()}. No podrá recibir nuevas asignaciones ni reactivarse desde el sistema. Su historial se conservará.`;
    if (!(await this.dialogos.confirmar(aviso))) return;
    this.guardandoSituacion.set(true);
    this.errorSituacion.set('');
    this.vehiculoService
      .registrarSituacion(unidad.id, {
        ...datos,
        confirmarBaja: datos.accion === 'BAJA',
      } as SituacionUnidadRequest)
      .subscribe({
        next: (mov) => {
          this.guardandoSituacion.set(false);
          this.movimientosUnidad.update((lista) => [mov, ...lista]);
          const nuevo = { ...unidad, estado: mov.estadoNuevo as EstadoVehiculo };
          this.unidadGestion.set(nuevo);
          this.vehiculos.update((lista) => lista.map((v) => (v.id === unidad.id ? nuevo : v)));
          this.mensajeExito.set(
            `Unidad ${unidad.placa}: ${this.nombreEstado(mov.estadoNuevo)}. Movimiento registrado.`,
          );
          this.situacionForm.controls.motivo.setValue('');
          this.situacionForm.controls.accion.setValue('BAJA');
        },
        error: (error) => {
          this.guardandoSituacion.set(false);
          this.errorSituacion.set(
            error.error?.mensaje ||
              error.error?.message ||
              'No se pudo registrar el movimiento. Conserva los datos y vuelve a intentarlo.',
          );
        },
      });
  }
  estadoOriginal: EstadoVehiculo = 'DISPONIBLE';
  mantenimientoForm;
  requiereMantenimiento(): boolean {
    return (
      this.vehiculoEditandoId() !== null &&
      this.vehiculoForm.controls.estado.value === 'MANTENIMIENTO' &&
      this.estadoOriginal !== 'MANTENIMIENTO'
    );
  }
  nombreEstado(estado: string): string {
    return (
      (
        {
          DISPONIBLE: 'Disponible',
          EN_SERVICIO: 'En operación',
          MANTENIMIENTO: 'En mantenimiento',
          INACTIVO: 'Pendiente de clasificar',
          SUSPENDIDO: 'Registro anterior de prueba',
          BAJA_DEFINITIVA: 'Fuera de servicio — baja definitiva',
        } as Record<string, string>
      )[estado] || estado
    );
  }
  readonly lecturasUnidad = signal<MovimientoOdometro[]>([]);
  readonly leyendoOdometro = signal(false);
  readonly errorOdometro = signal('');
  readonly historialOdometroVisible = signal(false);
  versionEditada: number | undefined;
  lecturaBaseEdicion: number | null = null;
  estadoBaseEdicion: string = 'Sin registro';
  puedeAjustarOdometro(): boolean {
    return ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(sessionStorage.getItem('rol') ?? '');
  }
  consultarOdometro(unidad: Vehiculo) {
    this.historialOdometroVisible.set(true);
    this.leyendoOdometro.set(true);
    this.errorOdometro.set('');
    this.lecturasUnidad.set([]);
    this.vehiculoService.historialOdometro(unidad.id).subscribe({
      next: (datos) => {
        if (this.unidadGestion()?.id === unidad.id) {
          this.lecturasUnidad.set(datos);
          this.leyendoOdometro.set(false);
        }
      },
      error: () => {
        if (this.unidadGestion()?.id === unidad.id) {
          this.errorOdometro.set('No se pudo consultar las lecturas. Vuelve a intentarlo.');
          this.leyendoOdometro.set(false);
        }
      },
    });
  }
  vehiculoForm;

  constructor(
    private vehiculoService: VehiculoService,
    private formBuilder: FormBuilder,
    private router: Router,
    private sesionService: SesionService,
  ) {
    this.situacionForm = this.formBuilder.nonNullable.group({
      accion: ['BAJA', [Validators.required]],
      fechaEfectiva: [this.hoySituacion, [Validators.required]],
      motivo: ['', [Validators.required, Validators.maxLength(1000)]],
      observaciones: ['', [Validators.maxLength(2000)]],
      referenciaSustento: ['', [Validators.maxLength(500)]],
    });
    this.mantenimientoForm = this.formBuilder.nonNullable.group({
      titulo: ['', [Validators.required, Validators.maxLength(150)]],
      tipoFalla: ['', [Validators.required]],
      prioridad: ['', [Validators.required]],
      descripcion: ['', [Validators.required, Validators.maxLength(1500)]],
      kilometrajeReporte: this.formBuilder.control<number | null>(null, [
        Validators.min(0),
        Validators.max(2147483647),
      ]),
    });
    this.vehiculoForm = this.formBuilder.nonNullable.group({
      placa: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{6,15}$/)]],
      marca: ['', [Validators.required]],
      modelo: ['', [Validators.required]],
      anioFabricacion: [2026, [Validators.required, Validators.min(1980), Validators.max(2100)]],
      tipo: ['CAMIONETA', [Validators.required]],
      capacidadPasajeros: [5, [Validators.required, Validators.min(1), Validators.max(100)]],
      estado: ['DISPONIBLE', [Validators.required]],
      odometroKm: this.formBuilder.control<number | null>(null, [
        Validators.min(0),
        Validators.max(99999999.99),
      ]),
      estadoOdometro: ['OPERATIVO'],
      motivoOdometro: ['', [Validators.maxLength(300)]],
      actualizarOdometro: [false],
    });
  }

  ngOnInit(): void {
    const cambios = this.vehiculoForm.valueChanges.subscribe(() => this.guardarBorrador());
    this.destroyRef.onDestroy(() => cambios.unsubscribe());
    this.cargarVehiculos();
  }

  async cargarVehiculos(): Promise<void> {
    this.cargando.set(true);
    this.mensajeError.set('');

    this.vehiculoService.listar().subscribe({
      next: (respuesta) => {
        this.vehiculos.set(respuesta);
        if (!this.enlaceProcesado && this.rutaActual) {
          const id = Number(this.rutaActual.snapshot.queryParamMap.get('unidad'));
          const accion = this.rutaActual.snapshot.queryParamMap.get('accion');
          if (id > 0) {
            this.enlaceProcesado = true;
            const unidad = respuesta.find((v) => v.id === id);
            if (unidad) this.abrirGestionUnidad(unidad, 'BAJA');
          }
        }

        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.mensajeError.set('No fue posible cargar los vehículos.');
      },
    });
  }

  abrirFormulario(): void {
    this.estadoOriginal = 'DISPONIBLE';
    this.mantenimientoForm.reset();
    this.vehiculoEditandoId.set(null);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.vehiculoForm.controls.estado.enable();
    this.vehiculoForm.reset({
      placa: '',
      marca: '',
      modelo: '',
      anioFabricacion: 2026,
      tipo: 'CAMIONETA',
      capacidadPasajeros: 5,
      estado: 'DISPONIBLE',
      odometroKm: null,
      estadoOdometro: 'OPERATIVO',
      motivoOdometro: '',
      actualizarOdometro: false,
    });

    this.borradorRestaurado.set(false);
    try {
      const clave = this.claveBorrador();
      const b = clave ? JSON.parse(sessionStorage.getItem(clave) || 'null') : null;
      if (b && Date.now() - b.fecha < 2 * 60 * 60 * 1000 && b.valores) {
        this.vehiculoForm.patchValue(b.valores, { emitEvent: false });
        this.borradorRestaurado.set(true);
      }
    } catch {
      /* Un borrador inválido no impide registrar la unidad. */
    }
    this.mostrarFormulario.set(true);
  }

  editarVehiculo(vehiculo: Vehiculo): void {
    this.vehiculoEditandoId.set(vehiculo.id);
    this.versionEditada = vehiculo.versionRegistro;
    this.lecturaBaseEdicion = vehiculo.odometroKm ?? null;
    this.estadoBaseEdicion = vehiculo.estadoOdometro ?? 'Sin base registrada';
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.estadoOriginal = vehiculo.estado;
    this.mantenimientoForm.reset();
    this.vehiculoForm.controls.estado.enable();
    this.vehiculoForm.setValue({
      placa: vehiculo.placa,
      marca: vehiculo.marca,
      modelo: vehiculo.modelo,
      anioFabricacion: vehiculo.anioFabricacion,
      tipo: vehiculo.tipo,
      capacidadPasajeros: vehiculo.capacidadPasajeros,
      estado: vehiculo.estado,
      odometroKm: vehiculo.odometroKm ?? null,
      estadoOdometro: vehiculo.estadoOdometro ?? 'OPERATIVO',
      motivoOdometro: '',
      actualizarOdometro: false,
    });

    this.mostrarFormulario.set(true);
  }

  cerrarFormulario(): void {
    const clave = this.claveBorrador();
    if (clave) sessionStorage.removeItem(clave);
    this.borradorRestaurado.set(false);
    this.mostrarFormulario.set(false);
    this.vehiculoEditandoId.set(null);

    this.vehiculoForm.controls.estado.enable();
    this.vehiculoForm.reset({
      placa: '',
      marca: '',
      modelo: '',
      anioFabricacion: 2026,
      tipo: 'CAMIONETA',
      capacidadPasajeros: 5,
      estado: 'DISPONIBLE',
      odometroKm: null,
      estadoOdometro: 'OPERATIVO',
      motivoOdometro: '',
      actualizarOdometro: false,
    });

    this.limpiarFotoSeleccionada();
  }

  seleccionarFoto(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0] ?? null;

    if (!archivo) return;

    if (!['image/jpeg', 'image/png'].includes(archivo.type)) {
      entrada.value = '';
      this.mensajeError.set('La foto debe ser JPG o PNG.');
      return;
    }

    if (archivo.size > 5 * 1024 * 1024) {
      entrada.value = '';
      this.mensajeError.set('La foto no puede superar los 5 MB.');
      return;
    }

    this.limpiarFotoSeleccionada();
    this.procesandoFoto.set(true);
    this.fotoOriginal.set(archivo);
    this.fotoSeleccionada.set(archivo);
    this.fotoPreview.set(URL.createObjectURL(archivo));
    this.mensajeError.set('');
  }

  quitarFotoSeleccionada(): void {
    this.limpiarFotoSeleccionada();
    const entrada = document.getElementById('fotoVehiculo') as HTMLInputElement | null;
    if (entrada) entrada.value = '';
  }

  guardarVehiculo(): void {
    if (this.procesandoFoto()) return;
    this.mensajeError.set('');
    this.mensajeExito.set('');

    if (this.vehiculoForm.invalid) {
      this.vehiculoForm.markAllAsTouched();
      this.mensajeError.set('Completa correctamente los campos obligatorios.');
      return;
    }

    const lectura = this.vehiculoForm.getRawValue();
    if (this.vehiculoEditandoId() === null || lectura.actualizarOdometro) {
      if (this.vehiculoEditandoId() !== null && !this.puedeAjustarOdometro()) {
        this.mensajeError.set('Solo el coordinador o administrador puede ajustar el odómetro.');
        return;
      }
      if (
        lectura.estadoOdometro === 'OPERATIVO' &&
        (lectura.odometroKm == null ||
          !Number.isFinite(lectura.odometroKm) ||
          Math.abs(lectura.odometroKm * 100 - Math.round(lectura.odometroKm * 100)) > 0.0001)
      ) {
        this.mensajeError.set('Ingresa el odómetro actual, con hasta dos decimales.');
        return;
      }
      if (
        (lectura.estadoOdometro === 'AVERIADO' || this.vehiculoEditandoId() !== null) &&
        lectura.motivoOdometro.trim().length < 5
      ) {
        this.mensajeError.set('Describe el motivo de la avería o ajuste (mínimo 5 caracteres).');
        return;
      }
    }
    if (this.requiereMantenimiento()) {
      this.mantenimientoForm.markAllAsTouched();
      const m = this.mantenimientoForm.getRawValue();
      if (
        this.mantenimientoForm.invalid ||
        !m.titulo.trim() ||
        !m.descripcion.trim() ||
        (m.kilometrajeReporte !== null && !Number.isInteger(m.kilometrajeReporte))
      ) {
        this.mensajeError.set(
          'Completa el título, tipo, prioridad y descripción del mantenimiento. El odómetro es opcional y debe ser entero.',
        );
        return;
      }
    }
    this.guardando.set(true);

    const datos: VehiculoRequest = {
      ...this.vehiculoForm.getRawValue(),
      codigoInterno: null,
      odometroKm:
        this.vehiculoForm.controls.estadoOdometro.value === 'AVERIADO'
          ? null
          : this.vehiculoForm.controls.odometroKm.value,
      versionRegistro: this.versionEditada,
    } as VehiculoRequest;

    const id = this.vehiculoEditandoId();

    const estadoObjetivo = datos.estado;
    const mantenimiento = this.requiereMantenimiento()
      ? ({ ...this.mantenimientoForm.getRawValue(), vehiculoId: id! } as MantenimientoRequest)
      : null;
    const operacionBase =
      id === null
        ? this.vehiculoService.registrar(datos)
        : this.vehiculoService.gestionar(
            id,
            { ...datos, estado: this.estadoOriginal },
            estadoObjetivo,
            mantenimiento,
          );

    const foto = this.fotoSeleccionada();
    let falloFoto = false;

    const operacion = operacionBase.pipe(
      switchMap((vehiculo) =>
        foto
          ? this.vehiculoService.guardarFoto(vehiculo.id, foto).pipe(
              catchError(() => {
                falloFoto = true;
                return of(vehiculo);
              }),
            )
          : [vehiculo],
      ),
    );

    operacion.subscribe({
      next: (vehiculoGuardado) => {
        this.guardando.set(false);

        const mensaje =
          id === null
            ? `Vehículo ${vehiculoGuardado.placa} registrado correctamente.`
            : `Vehículo ${vehiculoGuardado.placa} actualizado. Estado: ${this.nombreEstado(vehiculoGuardado.estado)}.`;

        this.cerrarFormulario();
        if (falloFoto)
          this.mensajeError.set(
            'Los datos y el estado de la unidad se guardaron, pero no se pudo guardar su fotografía. Puedes volver a adjuntarla desde Editar.',
          );
        this.mensajeExito.set(mensaje);
        this.cargarVehiculos();

        setTimeout(() => {
          this.mensajeExito.set('');
        }, 3000);
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);

        this.mensajeError.set(
          error.error?.mensaje ?? error.error?.message ?? 'No fue posible guardar el vehículo.',
        );
      },
    });
  }
  async desactivarVehiculo(vehiculo: Vehiculo): Promise<void> {
    if (vehiculo.estado === 'INACTIVO') {
      return;
    }

    const confirmar = await this.dialogos.confirmar(
      `¿Deseas marcar fuera de servicio el vehículo ${vehiculo.placa}?`,
    );

    if (!confirmar) {
      return;
    }

    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.vehiculoService.desactivar(vehiculo.id).subscribe({
      next: (vehiculoDesactivado) => {
        this.mensajeExito.set(`Vehículo ${vehiculoDesactivado.placa} marcado fuera de servicio.`);

        this.cargarVehiculos();

        setTimeout(() => {
          this.mensajeExito.set('');
        }, 3000);
      },
      error: (error: HttpErrorResponse) => {
        this.mensajeError.set(error.error?.message ?? 'No fue posible desactivar el vehículo.');
      },
    });
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irConductores(): void {
    this.router.navigate(['/conductores']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private claveBorrador() {
    const id = sessionStorage.getItem('usuarioId');
    return id ? 'transporte-borrador-vehiculo-' + id : null;
  }
  private guardarBorrador() {
    if (!this.mostrarFormulario() || this.vehiculoEditandoId() !== null) return;
    const clave = this.claveBorrador();
    if (!clave) return;
    try {
      sessionStorage.setItem(
        clave,
        JSON.stringify({ fecha: Date.now(), valores: this.vehiculoForm.getRawValue() }),
      );
    } catch {
      /* La cuota del navegador no bloquea el formulario. */
    }
  }
  ngOnDestroy() {
    this.guardarBorrador();
    this.limpiarFotoSeleccionada();
  }
  private limpiarFotoSeleccionada(): void {
    const preview = this.fotoPreview();

    if (preview) {
      URL.revokeObjectURL(preview);
    }

    this.fotoPreview.set(null);
    this.fotoSeleccionada.set(null);
    this.fotoOriginal.set(null);
    this.procesandoFoto.set(false);
  }
}
