import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { MapaUbicacion, PuntoMapa } from '../../components/mapa-ubicacion';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { RecorridoVialService } from '../../services/recorrido-vial.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  Ruta,
  RutaRequest,
  RutaService,
  Ubicacion,
  UbicacionRequest,
} from '../../services/ruta.service';
import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-rutas',
  imports: [CommonModule, ReactiveFormsModule, MapaUbicacion],
  templateUrl: './rutas.html',
  styleUrl: './rutas.scss',
})
export class Rutas implements OnInit, OnDestroy {
  private readonly dialogos = inyectarDialogos(DialogosService);
  rutas = signal<Ruta[]>([]);
  ubicaciones = signal<Ubicacion[]>([]);
  cargando = signal(true);
  guardando = signal(false);
  obteniendoUbicacion = signal(false);
  mostrarFormulario = signal(false);
  mostrarFormularioUbicacion = signal(false);
  rutaEditandoId = signal<number | null>(null);
  ubicacionEditandoId = signal<number | null>(null);
  distanciaGeografica = signal<number | null>(null);
  modoDistancia = signal<'CARRETERA' | 'GPS' | 'MANUAL'>('CARRETERA');
  calculandoRecorrido = signal(false);
  trayectoriaVial = signal<[number, number][]>([]);
  mensajeRecorrido = signal('');
  private peticionRecorrido?: AbortController;
  errorCarga = signal('');
  mensajeError = signal('');
  mensajeExito = signal('');

  rutaForm;
  ubicacionForm;

  constructor(
    private rutaService: RutaService,
    private fb: FormBuilder,
    private router: Router,
    private sesionService: SesionService,
    private recorridoService: RecorridoVialService,
  ) {
    this.rutaForm = this.fb.nonNullable.group({
      codigo: [''],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      origenUbicacionId: [0, [Validators.required, Validators.min(1)]],
      destinoUbicacionId: [0, [Validators.required, Validators.min(1)]],
      distanciaKm: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(0.01),
      ]),
      duracionEstimadaMinutos: [
        30,
        [Validators.required, Validators.min(1), Validators.max(10080)],
      ],
      observaciones: [''],
      estado: ['ACTIVA', [Validators.required]],
    });
    this.ubicacionForm = this.fb.nonNullable.group({
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      tipo: ['OTRO', [Validators.required]],
      latitud: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(-90),
        Validators.max(90),
      ]),
      longitud: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(-180),
        Validators.max(180),
      ]),
      referencia: ['', [Validators.maxLength(300)]],
      estado: ['ACTIVA', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.cargarDatos();
  }
  cargarRutas(): void {
    this.cargarDatos();
  }
  cargarDatos(): void {
    this.cargando.set(true);
    this.errorCarga.set('');
    forkJoin({
      rutas: this.rutaService.listar(),
      ubicaciones: this.rutaService.listarUbicaciones(),
    }).subscribe({
      next: (r) => {
        this.rutas.set(r.rutas);
        this.ubicaciones.set(r.ubicaciones);
        this.mensajeError.set('');
        this.errorCarga.set('');
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.errorCarga.set('No fue posible cargar rutas y ubicaciones.');
      },
    });
  }

  abrirFormulario(): void {
    this.limpiarRecorrido();
    this.rutaEditandoId.set(null);
    this.distanciaGeografica.set(null);
    this.modoDistancia.set('CARRETERA');
    this.mensajeError.set('');
    this.rutaForm.reset({
      codigo: '',
      nombre: '',
      origenUbicacionId: 0,
      destinoUbicacionId: 0,
      distanciaKm: null,
      duracionEstimadaMinutos: 30,
      observaciones: '',
      estado: 'ACTIVA',
    });
    this.mostrarFormulario.set(true);
  }
  editarRuta(ruta: Ruta): void {
    this.limpiarRecorrido();
    this.modoDistancia.set('MANUAL');
    this.rutaEditandoId.set(ruta.id);
    this.mensajeError.set('');
    this.rutaForm.setValue({
      codigo: ruta.codigo,
      nombre: ruta.nombre,
      origenUbicacionId: ruta.origenUbicacion?.id ?? 0,
      destinoUbicacionId: ruta.destinoUbicacion?.id ?? 0,
      distanciaKm: ruta.distanciaKm,
      duracionEstimadaMinutos: ruta.duracionEstimadaMinutos,
      observaciones: ruta.observaciones ?? '',
      estado: ruta.estado,
    });
    this.recalcularDistanciaReferencia(false);
    this.mostrarFormulario.set(true);
  }
  cerrarFormulario(): void {
    this.limpiarRecorrido();
    this.mostrarFormulario.set(false);
    this.rutaEditandoId.set(null);
    this.mensajeError.set('');
  }
  guardarRuta(): void {
    if (this.calculandoRecorrido() || this.guardando()) return;
    this.mensajeError.set('');
    if (this.rutaForm.invalid) {
      this.rutaForm.markAllAsTouched();
      this.mensajeError.set('Completa correctamente los campos obligatorios.');
      return;
    }
    const datos = this.rutaForm.getRawValue() as RutaRequest;
    if (datos.origenUbicacionId === datos.destinoUbicacionId) {
      this.mensajeError.set('El origen y el destino deben ser diferentes.');
      return;
    }
    const duplicada = this.rutas().find(
      (r) =>
        r.id !== this.rutaEditandoId() &&
        r.origenUbicacion?.id === datos.origenUbicacionId &&
        r.destinoUbicacion?.id === datos.destinoUbicacionId,
    );
    if (duplicada) {
      this.mensajeError.set(
        `Ya existe la ruta ${duplicada.codigo} con el mismo origen y destino. Selecciona otro recorrido o edita la ruta existente.`,
      );
      return;
    }
    this.guardando.set(true);
    const id = this.rutaEditandoId();
    const op =
      id === null ? this.rutaService.registrar(datos) : this.rutaService.actualizar(id, datos);
    op.subscribe({
      next: (ruta) => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.mensajeExito.set(`Ruta ${ruta.codigo} guardada correctamente.`);
        this.cargarDatos();
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(
          e.error?.mensaje ?? e.error?.message ?? 'No fue posible guardar la ruta.',
        );
      },
    });
  }

  abrirUbicacion(ubicacion?: Ubicacion): void {
    this.ubicacionEditandoId.set(ubicacion?.id ?? null);
    this.mensajeError.set('');
    this.ubicacionForm.reset({
      nombre: ubicacion?.nombre ?? '',
      tipo: ubicacion?.tipo ?? 'OTRO',
      latitud: ubicacion?.latitud ?? null,
      longitud: ubicacion?.longitud ?? null,
      referencia: ubicacion?.referencia ?? '',
      estado: ubicacion?.estado ?? 'ACTIVA',
    });
    this.mostrarFormularioUbicacion.set(true);
  }
  cerrarUbicacion(): void {
    this.mostrarFormularioUbicacion.set(false);
    this.ubicacionEditandoId.set(null);
  }
  guardarUbicacion(): void {
    if (this.ubicacionForm.invalid) {
      this.ubicacionForm.markAllAsTouched();
      this.mensajeError.set('Completa correctamente la ubicación.');
      return;
    }
    this.guardando.set(true);
    const datos = this.ubicacionForm.getRawValue() as UbicacionRequest;
    const id = this.ubicacionEditandoId();
    const op =
      id === null
        ? this.rutaService.registrarUbicacion(datos)
        : this.rutaService.actualizarUbicacion(id, datos);
    op.subscribe({
      next: (u) => {
        this.guardando.set(false);
        this.cerrarUbicacion();
        this.mensajeExito.set(`Ubicación ${u.nombre} guardada correctamente.`);
        this.cargarDatos();
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(
          e.error?.mensaje ?? e.error?.message ?? 'No fue posible guardar la ubicación.',
        );
      },
    });
  }
  get puntosUbicacion(): PuntoMapa[] {
    const { latitud, longitud } = this.ubicacionForm.getRawValue();
    return latitud != null && longitud != null
      ? [{ latitud, longitud, nombre: 'Ubicación seleccionada' }]
      : [];
  }
  get puntosRuta(): PuntoMapa[] {
    const valores = this.rutaForm.getRawValue();
    return [valores.origenUbicacionId, valores.destinoUbicacionId].flatMap((id, i) => {
      const u = this.ubicaciones().find((u) => u.id === Number(id));
      return u?.latitud != null && u.longitud != null
        ? [
            {
              latitud: u.latitud,
              longitud: u.longitud,
              nombre: `${i === 0 ? 'Origen' : 'Destino'}: ${u.nombre}`,
            },
          ]
        : [];
    });
  }
  seleccionarCoordenadas(punto: { latitud: number; longitud: number }): void {
    this.ubicacionForm.patchValue(punto);
    this.ubicacionForm.markAsDirty();
  }
  usarMiUbicacion(): void {
    if (!navigator.geolocation) {
      this.mensajeError.set('Este navegador no permite obtener la ubicación.');
      return;
    }
    this.obteniendoUbicacion.set(true);
    navigator.geolocation.getCurrentPosition(
      (p) => {
        this.ubicacionForm.patchValue({
          latitud: Number(p.coords.latitude.toFixed(7)),
          longitud: Number(p.coords.longitude.toFixed(7)),
        });
        this.obteniendoUbicacion.set(false);
      },
      () => {
        this.obteniendoUbicacion.set(false);
        this.mensajeError.set('No se pudo obtener la ubicación. Revisa el permiso del navegador.');
      },
      { enableHighAccuracy: true, timeout: 15000 },
    );
  }
  recalcularDistanciaReferencia(aplicar = true): void {
    this.limpiarRecorrido();
    if (aplicar && this.modoDistancia() === 'CARRETERA')
      this.rutaForm.controls.distanciaKm.setValue(null);
    const o = this.ubicaciones().find(
      (u) => u.id === Number(this.rutaForm.controls.origenUbicacionId.value),
    );
    const d = this.ubicaciones().find(
      (u) => u.id === Number(this.rutaForm.controls.destinoUbicacionId.value),
    );
    if (o?.latitud == null || o.longitud == null || d?.latitud == null || d.longitud == null) {
      this.distanciaGeografica.set(null);
      if (aplicar && this.modoDistancia() === 'GPS')
        this.rutaForm.controls.distanciaKm.setValue(null);
      return;
    }
    const rad = (v: number) => (v * Math.PI) / 180;
    const dLat = rad(d.latitud - o.latitud);
    const dLon = rad(d.longitud - o.longitud);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(rad(o.latitud)) * Math.cos(rad(d.latitud)) * Math.sin(dLon / 2) ** 2;
    const km = Number(
      (6371 * 2 * Math.atan2(Math.sqrt(Math.min(1, a)), Math.sqrt(Math.max(0, 1 - a)))).toFixed(2),
    );
    this.distanciaGeografica.set(km);
    if (aplicar && this.modoDistancia() === 'GPS')
      this.rutaForm.controls.distanciaKm.setValue(km > 0 ? km : null);
  }
  cambiarModoDistancia(modo: string): void {
    this.limpiarRecorrido();
    this.modoDistancia.set(modo === 'CARRETERA' ? 'CARRETERA' : modo === 'GPS' ? 'GPS' : 'MANUAL');
    if (modo === 'GPS') this.recalcularDistanciaReferencia();
    if (modo === 'CARRETERA') this.rutaForm.controls.distanciaKm.setValue(null);
  }
  usarDistanciaManual(): void {
    this.peticionRecorrido?.abort();
    this.peticionRecorrido = undefined;
    this.calculandoRecorrido.set(false);
    this.modoDistancia.set('MANUAL');
  }
  private limpiarRecorrido(): void {
    this.peticionRecorrido?.abort();
    this.peticionRecorrido = undefined;
    this.calculandoRecorrido.set(false);
    this.trayectoriaVial.set([]);
    this.mensajeRecorrido.set('');
  }
  async calcularPorCarretera(): Promise<void> {
    if (this.calculandoRecorrido()) return;
    this.limpiarRecorrido();
    const puntos = this.puntosRuta;
    if (
      puntos.length !== 2 ||
      this.rutaForm.controls.origenUbicacionId.value ===
        this.rutaForm.controls.destinoUbicacionId.value
    ) {
      this.mensajeRecorrido.set('Selecciona origen y destino diferentes, ambos con coordenadas.');
      return;
    }
    this.modoDistancia.set('CARRETERA');
    this.rutaForm.controls.distanciaKm.setValue(null);
    const peticion = new AbortController();
    this.peticionRecorrido = peticion;
    this.calculandoRecorrido.set(true);
    const timeout = setTimeout(() => peticion.abort(), 15000);
    try {
      const recorrido = await this.recorridoService.calcular(puntos[0], puntos[1], peticion.signal);
      if (this.peticionRecorrido !== peticion || peticion.signal.aborted) return;
      this.trayectoriaVial.set(recorrido.trayectoria);
      this.rutaForm.patchValue({
        distanciaKm: recorrido.distanciaKm,
        duracionEstimadaMinutos: recorrido.duracionMinutos,
      });
      this.mensajeRecorrido.set(
        'Recorrido encontrado. Revisa el trazado y ajusta la duración según el vehículo y las condiciones del camino.',
      );
    } catch (error) {
      if (this.peticionRecorrido !== peticion) return;
      this.modoDistancia.set('MANUAL');
      this.mensajeRecorrido.set(
        peticion.signal.aborted
          ? 'La consulta tardó demasiado. Vuelve a intentar o ingresa los kilómetros y la duración manualmente.'
          : error instanceof Error
            ? error.message
            : 'No se pudo calcular el recorrido. Ingresa los kilómetros y la duración manualmente.',
      );
    } finally {
      clearTimeout(timeout);
      if (this.peticionRecorrido === peticion) {
        this.calculandoRecorrido.set(false);
        this.peticionRecorrido = undefined;
      }
    }
  }
  ngOnDestroy(): void {
    this.limpiarRecorrido();
  }
  async desactivarRuta(ruta: Ruta): Promise<void> {
    if (
      ruta.estado === 'INACTIVA' ||
      !(await this.dialogos.confirmar(`¿Desactivar la ruta ${ruta.codigo}?`))
    )
      return;
    this.rutaService.desactivar(ruta.id).subscribe({ next: () => this.cargarDatos() });
  }
  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
  irVehiculos(): void {
    this.router.navigate(['/vehiculos']);
  }
  irConductores(): void {
    this.router.navigate(['/conductores']);
  }
  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }
}
