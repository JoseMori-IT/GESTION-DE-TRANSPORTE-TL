import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { HttpErrorResponse } from '@angular/common/http';

import { Router } from '@angular/router';

import {
  CargoAdministrativo,
  PersonalAdministrativo,
  PersonalAdministrativoService,
  RolAdministrativo,
} from '../../services/personal-administrativo.service';

import { SesionService } from '../../services/sesion.service';

import { SoloDigitosDirective } from '../../directives/solo-digitos.directive';

@Component({
  selector: 'app-personal-administrativo',
  imports: [ReactiveFormsModule, SoloDigitosDirective],
  templateUrl: './personal-administrativo.html',
  styleUrl: './personal-administrativo.scss',
})
export class PersonalAdministrativoPage implements OnInit {
  private readonly dialogos = inyectarDialogos(DialogosService);

  private readonly fb = inject(FormBuilder);

  private readonly router = inject(Router);

  private readonly personalApi = inject(PersonalAdministrativoService);

  private readonly sesionService = inject(SesionService);

  readonly personal = signal<PersonalAdministrativo[]>([]);

  readonly cargando = signal(true);

  readonly procesando = signal(false);

  readonly mensajeError = signal('');

  readonly mensajeExito = signal('');

  readonly filtro = signal('');

  readonly mostrarFormulario = signal(false);

  readonly mostrarDesactivacion = signal(false);

  readonly mostrarDetalle = signal(false);

  readonly cargandoDetalle = signal(false);

  readonly detallePersonal = signal<PersonalAdministrativo | null>(null);

  readonly personalEditando = signal<PersonalAdministrativo | null>(null);

  readonly personalSeleccionado = signal<PersonalAdministrativo | null>(null);

  readonly rolSesion = sessionStorage.getItem('rol') ?? '';

  readonly puedeCrearCoordinador =
    this.rolSesion === 'MASTER' || this.rolSesion === 'ADMINISTRADOR';

  readonly formulario = this.fb.nonNullable.group({
    dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],

    nombres: ['', [Validators.required, Validators.maxLength(120)]],

    apellidos: ['', [Validators.required, Validators.maxLength(120)]],

    correo: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(150),
        Validators.pattern(/^[A-Za-z0-9._%+-]+@palmas\.com\.pe$/i),
      ],
    ],

    contrasena: ['', [Validators.required, Validators.minLength(8)]],

    cargo: this.fb.nonNullable.control<CargoAdministrativo>(
      'AUXILIAR_TRANSPORTE',
      Validators.required,
    ),

    rol: this.fb.nonNullable.control<RolAdministrativo>('OPERADOR', Validators.required),

    fechaIngreso: [this.fechaActual(), Validators.required],
  });

  readonly formularioDesactivacion = this.fb.nonNullable.group({
    fechaCese: [this.fechaActual(), Validators.required],

    motivo: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]],
  });

  readonly personalFiltrado = computed(() => {
    const termino = this.filtro().trim().toLowerCase();

    if (!termino) {
      return this.personal();
    }

    return this.personal().filter((persona) => {
      const contenido = [
        persona.dni,
        persona.nombres,
        persona.apellidos,
        persona.correo,
        persona.cargo,
        persona.activo ? 'activo' : 'inactivo',
      ]
        .join(' ')
        .toLowerCase();

      return contenido.includes(termino);
    });
  });

  readonly totalActivos = computed(
    () => this.personal().filter((persona) => persona.activo).length,
  );

  readonly totalInactivos = computed(
    () => this.personal().filter((persona) => !persona.activo).length,
  );

  ngOnInit(): void {
    if (!this.validarAcceso()) {
      return;
    }

    this.cargarPersonal();
  }

  cargarPersonal(): void {
    this.cargando.set(true);
    this.mensajeError.set('');

    this.personalApi.listar().subscribe({
      next: (datos) => {
        const ordenados = [...datos].sort((primero, segundo) => segundo.id - primero.id);

        this.personal.set(ordenados);
        this.cargando.set(false);
      },

      error: (error) => {
        this.cargando.set(false);

        this.manejarError(error, 'No fue posible cargar el ' + 'personal administrativo.');
      },
    });
  }

  actualizarFiltro(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;

    this.filtro.set(entrada.value);
  }

  abrirNuevo(): void {
    this.personalEditando.set(null);

    const controlContrasena = this.formulario.controls.contrasena;

    controlContrasena.setValidators([Validators.required, Validators.minLength(8)]);

    this.formulario.reset({
      dni: '',
      nombres: '',
      apellidos: '',
      correo: '',
      contrasena: '',
      cargo: 'AUXILIAR_TRANSPORTE',
      rol: 'OPERADOR',
      fechaIngreso: this.fechaActual(),
    });

    controlContrasena.updateValueAndValidity();

    this.limpiarMensajes();
    this.mostrarFormulario.set(true);
  }

  abrirEditar(persona: PersonalAdministrativo): void {
    this.personalEditando.set(persona);

    const controlContrasena = this.formulario.controls.contrasena;

    controlContrasena.clearValidators();

    this.formulario.reset({
      dni: persona.dni,
      nombres: persona.nombres,
      apellidos: persona.apellidos,
      correo: persona.correo,
      contrasena: '',
      cargo: persona.cargo,
      rol: persona.rol,
      fechaIngreso: persona.fechaIngreso,
    });

    controlContrasena.updateValueAndValidity();

    this.limpiarMensajes();
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario(): void {
    if (this.procesando()) {
      return;
    }

    this.mostrarFormulario.set(false);
    this.personalEditando.set(null);
  }

  guardar(): void {
    if (this.formulario.invalid || this.procesando()) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.procesando.set(true);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    const datos = this.formulario.getRawValue();

    const persona = this.personalEditando();

    const solicitud = persona
      ? this.personalApi.actualizar(persona.id, {
          dni: datos.dni.trim(),

          nombres: datos.nombres.trim(),

          apellidos: datos.apellidos.trim(),

          correo: datos.correo.trim().toLowerCase(),

          cargo: datos.cargo,

          rol: datos.rol,

          fechaIngreso: datos.fechaIngreso,
        })
      : this.personalApi.registrar({
          dni: datos.dni.trim(),

          nombres: datos.nombres.trim(),

          apellidos: datos.apellidos.trim(),

          correo: datos.correo.trim().toLowerCase(),

          contrasena: datos.contrasena,

          cargo: datos.cargo,

          rol: datos.rol,

          fechaIngreso: datos.fechaIngreso,
        });

    solicitud.subscribe({
      next: (guardado) => {
        this.personal.update((lista) => {
          const existe = lista.some((item) => item.id === guardado.id);

          const actualizada = existe
            ? lista.map((item) => (item.id === guardado.id ? guardado : item))
            : [guardado, ...lista];

          return [...actualizada].sort((primero, segundo) => segundo.id - primero.id);
        });

        this.procesando.set(false);
        this.mostrarFormulario.set(false);
        this.personalEditando.set(null);

        this.mensajeExito.set(
          persona
            ? 'Los datos del personal se ' + 'actualizaron correctamente.'
            : 'El personal administrativo se ' + 'registró correctamente.',
        );
      },

      error: (error) => {
        this.procesando.set(false);

        this.manejarError(
          error,
          persona
            ? 'No fue posible actualizar ' + 'el personal.'
            : 'No fue posible registrar ' + 'el personal.',
        );
      },
    });
  }

  abrirDesactivacion(persona: PersonalAdministrativo): void {
    this.personalSeleccionado.set(persona);

    this.formularioDesactivacion.reset({
      fechaCese: this.fechaActual(),
      motivo: '',
    });

    this.limpiarMensajes();
    this.mostrarDesactivacion.set(true);
  }

  cerrarDesactivacion(): void {
    if (this.procesando()) {
      return;
    }

    this.mostrarDesactivacion.set(false);

    this.personalSeleccionado.set(null);
  }

  confirmarDesactivacion(): void {
    const persona = this.personalSeleccionado();

    if (!persona || this.formularioDesactivacion.invalid || this.procesando()) {
      this.formularioDesactivacion.markAllAsTouched();

      return;
    }

    this.procesando.set(true);
    this.mensajeError.set('');

    const datos = this.formularioDesactivacion.getRawValue();

    this.personalApi
      .desactivar(persona.id, {
        fechaCese: datos.fechaCese,

        motivo: datos.motivo.trim(),
      })
      .subscribe({
        next: (actualizado) => {
          this.reemplazarPersona(actualizado);

          this.procesando.set(false);
          this.mostrarDesactivacion.set(false);

          this.personalSeleccionado.set(null);

          this.mensajeExito.set('El acceso del personal fue ' + 'desactivado correctamente.');
        },

        error: (error) => {
          this.procesando.set(false);

          this.manejarError(error, 'No fue posible desactivar ' + 'el acceso.');
        },
      });
  }

  async reactivar(persona: PersonalAdministrativo): Promise<void> {
    if (this.procesando()) {
      return;
    }

    const confirmado = await this.dialogos.confirmar(
      `¿Deseas reactivar el acceso de ` + `${persona.nombres} ` + `${persona.apellidos}?`,
    );

    if (!confirmado) {
      return;
    }

    this.procesando.set(true);
    this.limpiarMensajes();

    this.personalApi
      .reactivar(persona.id, {
        fechaReingreso: this.fechaActual(),
      })
      .subscribe({
        next: (actualizado) => {
          this.reemplazarPersona(actualizado);

          this.procesando.set(false);

          this.mensajeExito.set('El acceso del personal fue ' + 'reactivado correctamente.');
        },

        error: (error) => {
          this.procesando.set(false);

          this.manejarError(error, 'No fue posible reactivar ' + 'el acceso.');
        },
      });
  }

  abrirDetalle(persona: PersonalAdministrativo): void {
    this.mostrarDetalle.set(true);
    this.cargandoDetalle.set(true);
    this.detallePersonal.set(null);
    this.mensajeError.set('');

    this.personalApi.buscarPorId(persona.id).subscribe({
      next: (detalle) => {
        this.detallePersonal.set(detalle);

        this.cargandoDetalle.set(false);
      },

      error: (error) => {
        this.cargandoDetalle.set(false);
        this.mostrarDetalle.set(false);

        this.manejarError(error, 'No fue posible consultar el ' + 'detalle del personal.');
      },
    });
  }

  cerrarDetalle(): void {
    this.mostrarDetalle.set(false);
    this.cargandoDetalle.set(false);
    this.detallePersonal.set(null);
  }

  formatearFechaHora(valor: string | null): string {
    if (!valor) {
      return 'No registrado';
    }

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
      return valor.replace('T', ' ').slice(0, 19);
    }

    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(fecha);
  }

  nombreCargo(cargo: CargoAdministrativo): string {
    const nombres: Record<CargoAdministrativo, string> = {
      SUPERVISOR_TRANSPORTE: 'Supervisor de transporte',

      OPERADOR_TRANSPORTE: 'Operador de transporte',

      AUXILIAR_TRANSPORTE: 'Auxiliar de transporte',
    };

    return nombres[cargo];
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irAuditoria(): void {
    this.router.navigate(['/auditoria']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }

  private reemplazarPersona(persona: PersonalAdministrativo): void {
    this.personal.update((lista) => lista.map((item) => (item.id === persona.id ? persona : item)));
  }

  private validarAcceso(): boolean {
    const rol = sessionStorage.getItem('rol');

    if (rol && !['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(rol)) {
      this.router.navigate(['/dashboard'], {
        replaceUrl: true,
      });

      return false;
    }

    return true;
  }

  puedeGestionar(persona: PersonalAdministrativo): boolean {
    if (this.puedeCrearCoordinador) {
      return (
        persona.rol === 'COORDINADOR' || persona.rol === 'OPERADOR' || persona.rol === 'CONSULTA'
      );
    }

    return persona.rol === 'OPERADOR' || persona.rol === 'CONSULTA';
  }

  private manejarError(error: HttpErrorResponse, mensajePredeterminado: string): void {
    if (error.status === 401) {
      this.sesionService.cerrarSesionLocal();

      return;
    }

    if (error.status === 403) {
      this.router.navigate(['/dashboard'], {
        replaceUrl: true,
      });

      return;
    }

    this.mensajeError.set(error.error?.mensaje || error.error?.message || mensajePredeterminado);
  }

  private limpiarMensajes(): void {
    this.mensajeError.set('');
    this.mensajeExito.set('');
  }

  private fechaActual(): string {
    const fecha = new Date();

    const desplazamiento = fecha.getTimezoneOffset() * 60000;

    return new Date(fecha.getTime() - desplazamiento).toISOString().slice(0, 10);
  }
}
