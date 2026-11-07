import { ConfiguracionOperativaService } from '../../services/configuracion-operativa.service';
import { ActivatedRoute } from '@angular/router';
import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { DatePipe, DecimalPipe } from '@angular/common';

import { HttpErrorResponse } from '@angular/common/http';

import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Router } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';

import {
  EstadoMantenimiento,
  EvidenciaMantenimiento,
  Mantenimiento as RegistroMantenimiento,
  MantenimientoService,
  PrioridadMantenimiento,
  TipoEvidenciaMantenimiento,
  TipoFalla,
} from '../../services/mantenimiento.service';

import { Vehiculo, VehiculoService } from '../../services/vehiculo.service';

import { SesionService } from '../../services/sesion.service';

type FiltroMantenimiento = 'TODOS' | EstadoMantenimiento;

type AccionMantenimiento = 'EVALUAR' | 'REPARAR' | 'CERRAR';

@Component({
  selector: 'app-mantenimiento',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './mantenimiento.html',
  styleUrl: './mantenimiento.scss',
})
export class Mantenimiento implements OnInit {
  readonly configuracion = inject(ConfiguracionOperativaService);
  private rutaPendiente = inject(ActivatedRoute, { optional: true });
  private pendienteAbierto = false;

  private readonly dialogos = inyectarDialogos(DialogosService);

  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  private readonly mantenimientoApi = inject(MantenimientoService);

  private readonly vehiculoApi = inject(VehiculoService);

  private readonly sesionService = inject(SesionService);

  readonly mantenimientos = signal<RegistroMantenimiento[]>([]);

  readonly vehiculos = signal<Vehiculo[]>([]);

  readonly cargando = signal(true);
  readonly guardando = signal(false);

  readonly filtro = signal<FiltroMantenimiento>('TODOS');

  readonly busquedaPlaca = signal('');

  readonly mostrarReporte = signal(false);
  readonly fotoReparacion = signal<File | null>(null);

  seleccionarFotoReparacion(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;
    if (
      !['image/jpeg', 'image/png', 'image/webp'].includes(archivo.type) ||
      archivo.size > 10 * 1024 * 1024
    ) {
      this.mensajeFormulario.set('Selecciona una foto JPG, PNG o WebP de hasta 10 MB.');
      input.value = '';
      return;
    }
    this.fotoReparacion.set(archivo);
    this.mensajeFormulario.set('');
  }

  readonly accion = signal<AccionMantenimiento | null>(null);

  readonly seleccionado = signal<RegistroMantenimiento | null>(null);

  /*
   * Evidencias de mantenimiento.
   */
  readonly mostrarEvidencias = signal(false);

  readonly mantenimientoEvidencias = signal<RegistroMantenimiento | null>(null);

  readonly evidencias = signal<EvidenciaMantenimiento[]>([]);

  readonly cargandoEvidencias = signal(false);
  readonly subiendoEvidencia = signal(false);

  readonly archivoEvidencia = signal<File | null>(null);

  readonly nombreArchivoEvidencia = computed(
    () => this.archivoEvidencia()?.name ?? 'Ningún archivo seleccionado',
  );

  /*
   * Reporte imprimible.
   */
  readonly mantenimientoReporte = signal<RegistroMantenimiento | null>(null);

  readonly evidenciasReporte = signal<EvidenciaMantenimiento[]>([]);

  readonly imagenesReporte = signal<Record<number, string>>({});

  readonly preparandoReporte = signal(false);

  readonly mensajeError = signal('');
  readonly mensajeExito = signal('');
  readonly mensajeFormulario = signal('');
  readonly mensajeEvidencia = signal('');
  readonly mensajeExitoEvidencia = signal('');

  readonly totalReportados = computed(() => this.contarEstado('REPORTADO'));

  readonly totalEnEvaluacion = computed(() => this.contarEstado('EN_EVALUACION'));

  readonly totalEnMantenimiento = computed(() => this.contarEstado('EN_MANTENIMIENTO'));

  readonly totalReparados = computed(() => this.contarEstado('REPARADO'));

  readonly totalCerrados = computed(() => this.contarEstado('CERRADO'));

  readonly vehiculosDisponibles = computed(() =>
    this.vehiculos().filter((vehiculo) => vehiculo.estado === 'DISPONIBLE'),
  );

  readonly mantenimientosFiltrados = computed(() => {
    const filtro = this.filtro();

    const busqueda = this.normalizarTexto(this.busquedaPlaca());

    return this.mantenimientos().filter((mantenimiento) => {
      const coincideEstado = filtro === 'TODOS' || mantenimiento.estado === filtro;

      const placa = this.normalizarTexto(mantenimiento.vehiculoPlaca);

      const codigo = this.normalizarTexto(mantenimiento.codigo);

      const titulo = this.normalizarTexto(mantenimiento.titulo);

      const coincideBusqueda =
        busqueda.length === 0 ||
        placa.includes(busqueda) ||
        codigo.includes(busqueda) ||
        titulo.includes(busqueda);

      return coincideEstado && coincideBusqueda;
    });
  });

  readonly formularioReporte = this.formBuilder.group({
    vehiculoId: this.formBuilder.control<number | null>(null, Validators.required),

    tipoFalla: this.formBuilder.control<TipoFalla | ''>('', Validators.required),

    prioridad: this.formBuilder.control<PrioridadMantenimiento | ''>('', Validators.required),

    titulo: this.formBuilder.control('', [Validators.required, Validators.maxLength(150)]),

    descripcion: this.formBuilder.control('', [Validators.required, Validators.maxLength(1500)]),

    kilometrajeReporte: this.formBuilder.control<number | null>(null, Validators.min(0)),
  });

  readonly formularioEvaluacion = this.formBuilder.group({
    diagnostico: this.formBuilder.control('', [Validators.required, Validators.maxLength(1500)]),

    tallerProveedor: this.formBuilder.control('', Validators.maxLength(150)),
    prioridad: this.formBuilder.control<PrioridadMantenimiento>('BAJA', Validators.required),
  });

  readonly formularioReparacion = this.formBuilder.group({
    trabajoRealizado: this.formBuilder.control('', [
      Validators.required,
      Validators.maxLength(1500),
    ]),

    costo: this.formBuilder.control<number | null>(null, Validators.min(0)),
  });

  readonly formularioCierre = this.formBuilder.group({
    observacionesCierre: this.formBuilder.control('', Validators.maxLength(1000)),
  });

  readonly formularioEvidencia = this.formBuilder.group({
    tipo: this.formBuilder.control<TipoEvidenciaMantenimiento>('FALLA_REPORTADA', {
      nonNullable: true,
      validators: Validators.required,
    }),

    descripcion: this.formBuilder.control('', Validators.maxLength(500)),
  });

  ngOnInit(): void {
    void this.configuracion.cargar();
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.mensajeError.set('');

    forkJoin({
      mantenimientos: this.mantenimientoApi.listar(),

      vehiculos: this.vehiculoApi.listar(),
    }).subscribe({
      next: ({ mantenimientos, vehiculos }) => {
        const ordenados = [...mantenimientos].sort((primero, segundo) => segundo.id - primero.id);

        this.mantenimientos.set(ordenados);
        const id = Number(this.rutaPendiente?.snapshot.queryParamMap.get('atender'));
        if (!this.pendienteAbierto && Number.isSafeInteger(id) && id > 0) {
          this.pendienteAbierto = true;
          const x = ordenados.find((v) => v.id === id);
          if (x) this.revisar(x);
          else this.mensajeError.set('El mantenimiento solicitado ya no está disponible.');
        }
        this.vehiculos.set(vehiculos);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        this.manejarErrorGeneral(error);
      },
    });
  }

  seleccionarFiltro(filtro: FiltroMantenimiento): void {
    this.filtro.set(filtro);
  }

  actualizarBusqueda(evento: Event): void {
    const elemento = evento.target as HTMLInputElement;

    this.busquedaPlaca.set(elemento.value);
  }

  limpiarBusqueda(): void {
    this.busquedaPlaca.set('');
  }

  abrirReporte(): void {
    this.formularioReporte.reset({
      vehiculoId: null,
      tipoFalla: '',
      prioridad: '',
      titulo: '',
      descripcion: '',
      kilometrajeReporte: null,
    });

    this.mensajeFormulario.set('');
    this.mostrarReporte.set(true);
  }

  cerrarReporte(): void {
    if (this.guardando()) {
      return;
    }

    this.mostrarReporte.set(false);
    this.mensajeFormulario.set('');
  }

  registrarReporte(): void {
    if (this.formularioReporte.invalid) {
      this.formularioReporte.markAllAsTouched();

      this.mensajeFormulario.set('Completa los campos obligatorios.');
      return;
    }

    const datos = this.formularioReporte.getRawValue();

    if (
      datos.vehiculoId === null ||
      datos.tipoFalla === null ||
      datos.tipoFalla === '' ||
      datos.prioridad === null ||
      datos.prioridad === ''
    ) {
      this.mensajeFormulario.set('Selecciona el vehículo, tipo y prioridad.');
      return;
    }

    this.guardando.set(true);
    this.mensajeFormulario.set('');

    this.mantenimientoApi
      .registrar({
        vehiculoId: datos.vehiculoId,
        tipoFalla: datos.tipoFalla,
        prioridad: datos.prioridad,
        titulo: datos.titulo?.trim() ?? '',
        descripcion: datos.descripcion?.trim() ?? '',
        kilometrajeReporte: datos.kilometrajeReporte,
      })
      .subscribe({
        next: (mantenimiento) => {
          this.guardando.set(false);
          this.mostrarReporte.set(false);

          this.mantenimientos.update((lista) => [mantenimiento, ...lista]);

          this.vehiculos.update((lista) =>
            lista.map((vehiculo) =>
              vehiculo.id === mantenimiento.vehiculoId
                ? {
                    ...vehiculo,
                    estado: 'MANTENIMIENTO',
                  }
                : vehiculo,
            ),
          );

          this.mensajeExito.set(`Se registró el reporte ` + `${mantenimiento.codigo}.`);
        },
        error: (error: HttpErrorResponse) => {
          this.guardando.set(false);

          this.mensajeFormulario.set(
            this.obtenerMensaje(error, 'No fue posible registrar la falla.'),
          );
        },
      });
  }

  abrirAccion(mantenimiento: RegistroMantenimiento, accion: AccionMantenimiento): void {
    this.fotoReparacion.set(null);
    this.seleccionado.set(mantenimiento);
    this.accion.set(accion);
    this.mensajeFormulario.set('');

    this.formularioEvaluacion.reset({
      diagnostico: mantenimiento.diagnostico ?? '',
      tallerProveedor: mantenimiento.tallerProveedor ?? '',
      prioridad: mantenimiento.prioridad,
    });

    this.formularioReparacion.reset({
      trabajoRealizado: mantenimiento.trabajoRealizado ?? '',
      costo: mantenimiento.costo,
    });

    this.formularioCierre.reset({
      observacionesCierre: mantenimiento.observacionesCierre ?? '',
    });
  }

  revisar(mantenimiento: RegistroMantenimiento): void {
    this.seleccionado.set(mantenimiento);
    this.accion.set(null);
    this.mensajeFormulario.set('');
  }

  cerrarAccion(): void {
    if (this.guardando()) {
      return;
    }

    this.accion.set(null);
    this.seleccionado.set(null);
    this.mensajeFormulario.set('');
  }

  evaluar(): void {
    const mantenimiento = this.seleccionado();

    if (!mantenimiento || this.formularioEvaluacion.invalid) {
      this.formularioEvaluacion.markAllAsTouched();

      this.mensajeFormulario.set('Registra el diagnóstico de la unidad.');
      return;
    }

    const datos = this.formularioEvaluacion.getRawValue();

    this.guardando.set(true);
    this.mensajeFormulario.set('');

    this.mantenimientoApi
      .evaluar(mantenimiento.id, {
        diagnostico: datos.diagnostico?.trim() ?? '',
        tallerProveedor: datos.tallerProveedor?.trim() || null,
        prioridad: datos.prioridad ?? 'BAJA',
      })
      .subscribe({
        next: (actualizado) => {
          this.completarAccion(actualizado, 'El reporte fue evaluado correctamente.');
        },
        error: (error: HttpErrorResponse) => {
          this.manejarErrorFormulario(error);
        },
      });
  }

  async iniciar(mantenimiento: RegistroMantenimiento): Promise<void> {
    if (this.guardando()) return;
    const confirmado = await this.dialogos.confirmar(
      `¿Deseas iniciar la atención de ` + `${mantenimiento.codigo}?`,
    );

    if (!confirmado || this.guardando()) {
      return;
    }

    this.guardando.set(true);
    this.mensajeError.set('');
    this.mensajeFormulario.set('');

    this.mantenimientoApi.iniciar(mantenimiento.id).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.reemplazar(actualizado);
        if (this.seleccionado()?.id === actualizado.id) {
          this.seleccionado.set(actualizado);
        }

        this.mensajeExito.set('La atención de mantenimiento ' + 'fue iniciada.');
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);
        const mensaje = this.obtenerMensaje(error, 'No fue posible iniciar la atención.');
        this.mensajeError.set(mensaje);
        this.mensajeFormulario.set(mensaje);
      },
    });
  }

  reparar(): void {
    const mantenimiento = this.seleccionado();

    if (!mantenimiento || this.formularioReparacion.invalid) {
      this.formularioReparacion.markAllAsTouched();

      this.mensajeFormulario.set('Registra el trabajo realizado.');
      return;
    }

    const datos = this.formularioReparacion.getRawValue();

    this.guardando.set(true);
    this.mensajeFormulario.set('');

    this.mantenimientoApi
      .reparar(mantenimiento.id, {
        trabajoRealizado: datos.trabajoRealizado?.trim() ?? '',
        costo: datos.costo,
      })
      .subscribe({
        next: (actualizado) => {
          const foto = this.fotoReparacion();
          if (!foto) {
            this.completarAccion(actualizado, 'La reparación fue registrada.');
            return;
          }
          this.mantenimientoApi
            .registrarEvidencia(
              actualizado.id,
              'REPARACION',
              datos.trabajoRealizado?.trim() ?? '',
              foto,
            )
            .subscribe({
              next: () => {
                this.fotoReparacion.set(null);
                this.completarAccion(
                  actualizado,
                  'La reparación y su fotografía fueron registradas. La foto aparecerá en el reporte.',
                );
              },
              error: (error: HttpErrorResponse) => {
                this.guardando.set(false);
                this.reemplazar(actualizado);
                this.accion.set(null);
                this.seleccionado.set(actualizado);
                this.mensajeFormulario.set(
                  'La reparación se guardó, pero la foto no se pudo cargar. Puedes adjuntarla desde Evidencias; no necesitas registrar la reparación nuevamente. ' +
                    this.obtenerMensaje(error, 'Error al cargar la fotografía.'),
                );
              },
            });
        },
        error: (error: HttpErrorResponse) => {
          this.manejarErrorFormulario(error);
        },
      });
  }

  cerrar(): void {
    const mantenimiento = this.seleccionado();

    if (!mantenimiento || this.formularioCierre.invalid) {
      return;
    }

    const datos = this.formularioCierre.getRawValue();

    this.guardando.set(true);
    this.mensajeFormulario.set('');

    this.mantenimientoApi
      .cerrar(mantenimiento.id, {
        observacionesCierre: datos.observacionesCierre?.trim() || null,
      })
      .subscribe({
        next: (actualizado) => {
          this.vehiculos.update((lista) =>
            lista.map((vehiculo) =>
              vehiculo.id === actualizado.vehiculoId
                ? {
                    ...vehiculo,
                    estado: 'DISPONIBLE',
                  }
                : vehiculo,
            ),
          );

          this.completarAccion(
            actualizado,
            'El mantenimiento fue cerrado ' + 'y la unidad quedó disponible.',
          );
        },
        error: (error: HttpErrorResponse) => {
          this.manejarErrorFormulario(error);
        },
      });
  }

  async cancelar(mantenimiento: RegistroMantenimiento): Promise<void> {
    const confirmado = await this.dialogos.confirmar(
      `¿Deseas cancelar el mantenimiento ` + `${mantenimiento.codigo}?`,
    );

    if (!confirmado || this.guardando()) {
      return;
    }

    this.guardando.set(true);
    this.mensajeError.set('');

    this.mantenimientoApi.cancelar(mantenimiento.id).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.reemplazar(actualizado);

        this.vehiculos.update((lista) =>
          lista.map((vehiculo) =>
            vehiculo.id === actualizado.vehiculoId
              ? {
                  ...vehiculo,
                  estado: 'DISPONIBLE',
                }
              : vehiculo,
          ),
        );

        this.mensajeExito.set('El mantenimiento fue cancelado.');
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);

        this.mensajeError.set(
          this.obtenerMensaje(error, 'No fue posible cancelar el mantenimiento.'),
        );
      },
    });
  }

  /*
   * Abre el módulo de evidencias y consulta
   * los archivos registrados.
   */
  abrirEvidencias(mantenimiento: RegistroMantenimiento): void {
    // Si las evidencias se abren desde el detalle, cerrar primero
    // ese modal para evitar que quede apilado detrás.
    this.accion.set(null);
    this.seleccionado.set(null);

    this.mantenimientoEvidencias.set(mantenimiento);

    this.evidencias.set([]);
    this.archivoEvidencia.set(null);

    this.mensajeEvidencia.set('');
    this.mensajeExitoEvidencia.set('');

    this.formularioEvidencia.reset({
      tipo: this.tipoSugeridoEvidencia(mantenimiento.estado),
      descripcion: '',
    });

    this.mostrarEvidencias.set(true);
    this.cargarEvidencias(mantenimiento.id);
  }

  cerrarEvidencias(): void {
    if (this.subiendoEvidencia()) {
      return;
    }

    this.mostrarEvidencias.set(false);
    this.mantenimientoEvidencias.set(null);
    this.evidencias.set([]);
    this.archivoEvidencia.set(null);
    this.mensajeEvidencia.set('');
    this.mensajeExitoEvidencia.set('');
  }

  cargarEvidencias(mantenimientoId?: number): void {
    const id = mantenimientoId ?? this.mantenimientoEvidencias()?.id;

    if (!id) {
      return;
    }

    this.cargandoEvidencias.set(true);
    this.mensajeEvidencia.set('');

    this.mantenimientoApi.listarEvidencias(id).subscribe({
      next: (evidencias) => {
        this.evidencias.set([...evidencias].sort((primera, segunda) => segunda.id - primera.id));

        this.cargandoEvidencias.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargandoEvidencias.set(false);

        this.mensajeEvidencia.set(
          this.obtenerMensaje(error, 'No fue posible cargar las evidencias.'),
        );
      },
    });
  }

  seleccionarArchivoEvidencia(evento: Event): void {
    const input = evento.target as HTMLInputElement;

    const archivo = input.files?.item(0) ?? null;

    this.mensajeEvidencia.set('');
    this.mensajeExitoEvidencia.set('');

    if (!archivo) {
      this.archivoEvidencia.set(null);
      return;
    }

    const maximoBytes = 10 * 1024 * 1024;

    const tiposPermitidos = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

    if (!tiposPermitidos.includes(archivo.type)) {
      this.archivoEvidencia.set(null);
      input.value = '';

      this.mensajeEvidencia.set('Solo se permiten imágenes JPG, PNG, WEBP o documentos PDF.');
      return;
    }

    if (archivo.size > maximoBytes) {
      this.archivoEvidencia.set(null);
      input.value = '';

      this.mensajeEvidencia.set('El archivo no puede superar los 10 MB.');
      return;
    }

    this.archivoEvidencia.set(archivo);
  }

  registrarEvidencia(): void {
    const mantenimiento = this.mantenimientoEvidencias();

    const archivo = this.archivoEvidencia();

    if (!mantenimiento) {
      return;
    }

    if (this.formularioEvidencia.invalid) {
      this.formularioEvidencia.markAllAsTouched();

      this.mensajeEvidencia.set('Selecciona el tipo de evidencia.');
      return;
    }

    if (!archivo) {
      this.mensajeEvidencia.set('Selecciona una fotografía o documento.');
      return;
    }

    const datos = this.formularioEvidencia.getRawValue();

    this.subiendoEvidencia.set(true);
    this.mensajeEvidencia.set('');
    this.mensajeExitoEvidencia.set('');

    this.mantenimientoApi
      .registrarEvidencia(mantenimiento.id, datos.tipo, datos.descripcion?.trim() ?? '', archivo)
      .subscribe({
        next: (evidencia) => {
          this.evidencias.update((lista) => [evidencia, ...lista]);

          this.subiendoEvidencia.set(false);
          this.archivoEvidencia.set(null);

          this.formularioEvidencia.patchValue({
            descripcion: '',
          });

          this.mensajeExitoEvidencia.set('La evidencia se registró correctamente.');
        },
        error: (error: HttpErrorResponse) => {
          this.subiendoEvidencia.set(false);

          this.mensajeEvidencia.set(
            this.obtenerMensaje(error, 'No fue posible registrar la evidencia.'),
          );
        },
      });
  }

  /*
   * La ventana se abre inmediatamente para evitar
   * bloqueos de ventanas emergentes en Brave/Chrome.
   */
  verEvidencia(evidencia: EvidenciaMantenimiento): void {
    const ventana = window.open('', '_blank');

    if (!ventana) {
      this.mensajeEvidencia.set(
        'El navegador bloqueó la apertura del archivo. ' +
          'Permite las ventanas emergentes para este sitio.',
      );
      return;
    }

    ventana.document.title = evidencia.nombreOriginal;

    ventana.document.body.innerHTML =
      '<p style="font-family:Arial;padding:24px">' + 'Cargando archivo...' + '</p>';

    this.mantenimientoApi.obtenerArchivoEvidencia(evidencia.id).subscribe({
      next: (archivo) => {
        const tipoContenido = this.tipoContenidoVisualizable(evidencia.tipoContenido);

        if (!tipoContenido) {
          ventana.close();

          this.mensajeEvidencia.set('Este tipo de archivo no admite vista previa.');
          return;
        }

        /*
         * El backend entrega la descarga como
         * application/octet-stream por seguridad.
         * Se reconstruye localmente con el tipo ya
         * validado al registrar la evidencia.
         */
        const archivoVisualizable = new Blob([archivo], {
          type: tipoContenido,
        });

        const url = URL.createObjectURL(archivoVisualizable);

        this.mostrarVistaPrevia(ventana, evidencia, url, tipoContenido);
      },
      error: (error: HttpErrorResponse) => {
        ventana.close();

        this.mensajeEvidencia.set(this.obtenerMensaje(error, 'No fue posible abrir el archivo.'));
      },
    });
  }

  private tipoContenidoVisualizable(tipoContenido: string): string | null {
    const tipo = tipoContenido.trim().toLowerCase();

    const permitidos = new Set(['image/jpeg', 'image/png', 'image/webp', 'application/pdf']);

    return permitidos.has(tipo) ? tipo : null;
  }

  private mostrarVistaPrevia(
    ventana: Window,
    evidencia: EvidenciaMantenimiento,
    url: string,
    tipoContenido: string,
  ): void {
    const documento = ventana.document;

    documento.open();
    documento.write(`
      <!doctype html>
      <html lang="es">
        <head>
          <meta charset="utf-8">
          <meta
            name="viewport"
            content="width=device-width,initial-scale=1"
          >
          <title>Vista previa de evidencia</title>
          <style>
            * { box-sizing: border-box; }
            html, body { width: 100%; min-height: 100%; margin: 0; }
            body {
              display: grid;
              grid-template-rows: auto minmax(0, 1fr);
              height: 100vh;
              color: #173d2b;
              background: #eef2e9;
              font-family: Arial, sans-serif;
            }
            header {
              display: flex;
              align-items: center;
              justify-content: space-between;
              gap: 18px;
              padding: 14px 22px;
              background: #ffffff;
              border-bottom: 1px solid #d7dfd2;
              box-shadow: 0 4px 18px rgba(20, 61, 43, .08);
            }
            .file { min-width: 0; }
            .file strong,
            .file small { display: block; }
            .file strong {
              overflow: hidden;
              font-size: 15px;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
            .file small { margin-top: 3px; color: #6b7c70; }
            a {
              flex: 0 0 auto;
              padding: 10px 16px;
              color: #ffffff;
              background: #1d6545;
              border-radius: 8px;
              font-size: 13px;
              font-weight: 700;
              text-decoration: none;
            }
            main {
              display: grid;
              min-height: 0;
              place-items: center;
              padding: 20px;
            }
            img {
              display: block;
              max-width: 100%;
              max-height: 100%;
              object-fit: contain;
              background: #ffffff;
              border-radius: 10px;
              box-shadow: 0 12px 34px rgba(20, 61, 43, .14);
            }
            iframe {
              width: 100%;
              height: 100%;
              background: #ffffff;
              border: 0;
              border-radius: 8px;
            }
            @media (max-width: 560px) {
              header { align-items: stretch; flex-direction: column; }
              a { text-align: center; }
              main { padding: 10px; }
            }
          </style>
        </head>
        <body>
          <header>
            <div class="file">
              <strong id="nombre-archivo"></strong>
              <small>Vista previa de evidencia de mantenimiento</small>
            </div>
            <a id="descargar-archivo">Descargar archivo</a>
          </header>
          <main id="visor"></main>
        </body>
      </html>
    `);
    documento.close();

    documento.title = evidencia.nombreOriginal;

    const nombre = documento.getElementById('nombre-archivo');

    if (nombre) {
      nombre.textContent = evidencia.nombreOriginal;
    }

    const descargar = documento.getElementById('descargar-archivo') as HTMLAnchorElement | null;

    if (descargar) {
      descargar.href = url;
      descargar.download = evidencia.nombreOriginal;
    }

    const visor = documento.getElementById('visor');

    if (visor) {
      if (tipoContenido === 'application/pdf') {
        const marco = documento.createElement('iframe');
        marco.src = url;
        marco.title = evidencia.nombreOriginal;
        visor.appendChild(marco);
      } else {
        const imagen = documento.createElement('img');
        imagen.src = url;
        imagen.alt = evidencia.nombreOriginal;
        visor.appendChild(imagen);
      }
    }

    ventana.addEventListener('beforeunload', () => URL.revokeObjectURL(url), { once: true });
  }

  imprimirMantenimiento(mantenimiento: RegistroMantenimiento): void {
    if (this.preparandoReporte()) {
      return;
    }

    this.preparandoReporte.set(true);

    this.revocarImagenesReporte();

    this.mantenimientoReporte.set(mantenimiento);

    this.evidenciasReporte.set([]);

    this.imagenesReporte.set({});

    this.mantenimientoApi.listarEvidencias(mantenimiento.id).subscribe({
      next: (evidencias) => {
        this.evidenciasReporte.set(evidencias);

        this.cargarImagenesReporte(evidencias);
      },
      error: () => {
        this.preparandoReporte.set(false);

        /*
         * Si no pueden consultarse las evidencias,
         * se imprime el resto del reporte.
         */
        this.ejecutarImpresion();
      },
    });
  }
  private cargarImagenesReporte(evidencias: EvidenciaMantenimiento[]): void {
    const imagenes = evidencias.filter((evidencia) => this.esImagen(evidencia));

    if (imagenes.length === 0) {
      this.preparandoReporte.set(false);
      this.ejecutarImpresion();
      return;
    }

    const solicitudes = imagenes.map((evidencia) =>
      this.mantenimientoApi.obtenerArchivoEvidencia(evidencia.id).pipe(
        map((archivo) => ({
          evidenciaId: evidencia.id,
          url: URL.createObjectURL(archivo),
        })),
        catchError(() => of(null)),
      ),
    );

    forkJoin(solicitudes).subscribe({
      next: (resultados) => {
        const urls: Record<number, string> = {};

        for (const resultado of resultados) {
          if (resultado) {
            urls[resultado.evidenciaId] = resultado.url;
          }
        }

        this.imagenesReporte.set(urls);
        this.preparandoReporte.set(false);
        this.ejecutarImpresion();
      },
      error: () => {
        this.preparandoReporte.set(false);
        this.ejecutarImpresion();
      },
    });
  }

  private ejecutarImpresion(): void {
    const limpiarDespuesDeImprimir = () => {
      this.revocarImagenesReporte();
    };

    window.addEventListener('afterprint', limpiarDespuesDeImprimir, {
      once: true,
    });

    /*
     * Espera a que Angular coloque las imágenes
     * descargadas dentro del reporte.
     */
    window.setTimeout(() => window.print(), 700);
  }

  private revocarImagenesReporte(): void {
    const urls = Object.values(this.imagenesReporte());

    for (const url of urls) {
      URL.revokeObjectURL(url);
    }

    this.imagenesReporte.set({});
  }

  etiquetaEstado(estado: EstadoMantenimiento): string {
    const etiquetas: Record<EstadoMantenimiento, string> = {
      REPORTADO: 'Reportado',
      EN_EVALUACION: 'En evaluación',
      EN_MANTENIMIENTO: 'En mantenimiento',
      REPARADO: 'Reparado',
      CERRADO: 'Cerrado',
      CANCELADO: 'Cancelado',
    };

    return etiquetas[estado];
  }

  etiquetaTipo(tipo: TipoFalla): string {
    const etiquetas: Record<TipoFalla, string> = {
      MOTOR: 'Motor',
      TRANSMISION: 'Transmisión',
      FRENOS: 'Frenos',
      SUSPENSION: 'Suspensión',
      DIRECCION: 'Dirección',
      SISTEMA_ELECTRICO: 'Sistema eléctrico',
      NEUMATICOS: 'Neumáticos',
      CARROCERIA: 'Carrocería',
      AIRE_ACONDICIONADO: 'Aire acondicionado',
      MANTENIMIENTO_PREVENTIVO: 'Mantenimiento preventivo',
      DOCUMENTACION: 'Documentación',
      OTRO: 'Otro',
    };

    return etiquetas[tipo];
  }

  etiquetaPrioridad(prioridad: PrioridadMantenimiento): string {
    const etiquetas: Record<PrioridadMantenimiento, string> = {
      BAJA: 'Baja',
      MEDIA: 'Media',
      ALTA: 'Alta',
      CRITICA: 'Crítica',
    };

    return etiquetas[prioridad];
  }

  etiquetaEvidencia(tipo: TipoEvidenciaMantenimiento): string {
    const etiquetas: Record<TipoEvidenciaMantenimiento, string> = {
      FALLA_REPORTADA: 'Falla reportada',
      DIAGNOSTICO: 'Diagnóstico',
      REPARACION: 'Reparación',
      CONFORMIDAD: 'Conformidad',
    };

    return etiquetas[tipo];
  }

  formatoTamanio(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  esImagen(evidencia: EvidenciaMantenimiento): boolean {
    return evidencia.tipoContenido.startsWith('image/');
  }

  urlImagenReporte(evidenciaId: number): string | null {
    return this.imagenesReporte()[evidenciaId] ?? null;
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private tipoSugeridoEvidencia(estado: EstadoMantenimiento): TipoEvidenciaMantenimiento {
    if (estado === 'REPORTADO') {
      return 'FALLA_REPORTADA';
    }

    if (estado === 'EN_EVALUACION') {
      return 'DIAGNOSTICO';
    }

    if (estado === 'EN_MANTENIMIENTO') {
      return 'REPARACION';
    }

    if (estado === 'REPARADO') {
      return 'CONFORMIDAD';
    }

    return 'CONFORMIDAD';
  }

  private contarEstado(estado: EstadoMantenimiento): number {
    return this.mantenimientos().filter((mantenimiento) => mantenimiento.estado === estado).length;
  }

  private reemplazar(actualizado: RegistroMantenimiento): void {
    this.mantenimientos.update((lista) =>
      lista.map((mantenimiento) =>
        mantenimiento.id === actualizado.id ? actualizado : mantenimiento,
      ),
    );
  }

  private completarAccion(actualizado: RegistroMantenimiento, mensaje: string): void {
    this.guardando.set(false);
    this.reemplazar(actualizado);
    this.accion.set(null);
    this.seleccionado.set(null);
    this.mensajeFormulario.set('');
    this.mensajeExito.set(mensaje);
  }

  private manejarErrorFormulario(error: HttpErrorResponse): void {
    this.guardando.set(false);

    this.mensajeFormulario.set(
      this.obtenerMensaje(error, 'No fue posible completar la operación.'),
    );
  }

  private manejarErrorGeneral(error: HttpErrorResponse): void {
    /*
     * El token expiró o dejó de ser válido.
     * Se limpia localmente porque el backend ya no
     * aceptará una petición de cierre de sesión.
     */
    if (error.status === 401) {
      this.sesionService.cerrarSesionLocal();
      return;
    }

    /*
     * El usuario sigue autenticado, pero no tiene
     * autorización para ejecutar la operación.
     */
    if (error.status === 403) {
      this.mensajeError.set(
        error.error?.mensaje ||
          error.error?.message ||
          'No tienes permisos para realizar esta operación.',
      );

      return;
    }

    this.mensajeError.set(this.obtenerMensaje(error, 'Ocurrió un error al procesar la operación.'));
  }

  private obtenerMensaje(error: HttpErrorResponse, predeterminado: string): string {
    return (
      error.error?.mensaje ||
      error.error?.message ||
      (error.status ? `${predeterminado} (código ${error.status}).` : '') ||
      predeterminado
    );
  }

  private normalizarTexto(valor: string): string {
    return valor
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}
