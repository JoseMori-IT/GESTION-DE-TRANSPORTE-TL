import { inject as inyectarDialogos } from '@angular/core';
import { DialogosService } from '../../services/dialogos.service';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import {
  Conductor,
  CuentaChofer,
  AccesoTemporalChofer,
  ConductorRequest,
  ConductorService,
} from '../../services/conductor.service';

import { SesionService } from '../../services/sesion.service';

import { SoloDigitosDirective } from '../../directives/solo-digitos.directive';

@Component({
  selector: 'app-conductores',
  imports: [CommonModule, ReactiveFormsModule, SoloDigitosDirective],
  templateUrl: './conductores.html',
  styleUrl: './conductores.scss',
})
export class Conductores implements OnInit {
  private readonly dialogos = inyectarDialogos(DialogosService);

  readonly puedeAdministrarCuentas = ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(
    sessionStorage.getItem('rol') || '',
  );

  conductores = signal<Conductor[]>([]);
  cargando = signal(true);
  guardando = signal(false);
  mostrarFormulario = signal(false);
  conductorEditandoId = signal<number | null>(null);
  mensajeError = signal('');
  errorCarga = signal('');
  mensajeExito = signal('');
  cuentasChofer = signal<CuentaChofer[]>([]);
  cargandoCuentas = signal(true);
  errorCuentas = signal('');
  accesoTemporal = signal<AccesoTemporalChofer | null>(null);
  procesandoCuentaId = signal<number | null>(null);

  conductorForm;

  constructor(
    private conductorService: ConductorService,
    private formBuilder: FormBuilder,
    private router: Router,
    private sesionService: SesionService,
  ) {
    this.conductorForm = this.formBuilder.nonNullable.group({
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      nombres: ['', [Validators.required]],
      apellidos: ['', [Validators.required]],
      telefono: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
      correo: ['', [Validators.email]],
      numeroLicencia: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{6,20}$/)]],
      categoriaLicencia: ['A_I', [Validators.required]],
      vencimientoLicencia: ['', [Validators.required]],
      estado: ['DISPONIBLE', [Validators.required]],
      crearCuenta: [this.puedeAdministrarCuentas],
    });
  }

  ngOnInit(): void {
    this.cargarConductores();
    this.cargarCuentas();
  }

  cargarConductores(): void {
    this.cargando.set(true);
    this.errorCarga.set('');

    this.conductorService.listar().subscribe({
      next: (respuesta) => {
        this.conductores.set(respuesta);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorCarga.set(
          error.status === 0
            ? 'No se pudo conectar con el servidor. Comprueba que el backend esté iniciado.'
            : error.status === 401
              ? 'Tu sesión venció o fue revocada. Vuelve a iniciar sesión.'
              : error.status === 403
                ? 'La cuenta no tiene permiso para consultar el personal.'
                : `No fue posible cargar los conductores (HTTP ${error.status}). ${error.error?.mensaje ?? ''}`.trim(),
        );
      },
    });
  }

  abrirFormulario(): void {
    this.conductorEditandoId.set(null);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.conductorForm.reset({
      dni: '',
      nombres: '',
      apellidos: '',
      telefono: '',
      correo: '',
      numeroLicencia: '',
      categoriaLicencia: 'A_I',
      vencimientoLicencia: '',
      estado: 'DISPONIBLE',
      crearCuenta: this.puedeAdministrarCuentas,
    });

    this.mostrarFormulario.set(true);
  }

  editarConductor(conductor: Conductor): void {
    this.conductorEditandoId.set(conductor.id);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.conductorForm.setValue({
      dni: conductor.dni,
      nombres: conductor.nombres,
      apellidos: conductor.apellidos,
      telefono: conductor.telefono,
      correo: conductor.correo ?? '',
      numeroLicencia: conductor.numeroLicencia,
      categoriaLicencia: conductor.categoriaLicencia,
      vencimientoLicencia: conductor.vencimientoLicencia,
      estado: conductor.estado,
      crearCuenta: false,
    });

    this.mostrarFormulario.set(true);
  }

  cerrarFormulario(): void {
    this.mostrarFormulario.set(false);
    this.conductorEditandoId.set(null);
  }

  guardarConductor(): void {
    if (this.guardando()) return;
    this.mensajeError.set('');
    this.mensajeExito.set('');

    if (this.conductorForm.invalid) {
      this.conductorForm.markAllAsTouched();
      this.mensajeError.set(
        Object.keys(this.conductorForm.controls)
          .map((campo) => this.errorCampo(campo))
          .filter(Boolean)
          .join(' '),
      );
      return;
    }

    const dni = this.conductorForm.controls.dni.value.trim();
    if (this.conductores().some((c) => c.dni === dni && c.id !== this.conductorEditandoId())) {
      void this.dialogos.informar(
        'Este usuario ya existe: el DNI ingresado ya está registrado en Conductores. Revisa el registro existente o corrige el DNI.',
      );
      return;
    }

    this.guardando.set(true);

    const valores = this.conductorForm.getRawValue();
    const { crearCuenta, ...datosFormulario } = valores;
    const datos = datosFormulario as ConductorRequest;

    const id = this.conductorEditandoId();

    const operacion =
      id === null
        ? this.conductorService.registrar(datos)
        : this.conductorService.actualizar(id, datos);

    operacion.subscribe({
      next: (conductorGuardado) => {
        if (id === null && crearCuenta) {
          this.conductorService.crearAcceso(conductorGuardado.id).subscribe({
            next: (acceso) => {
              this.accesoTemporal.set(acceso);
              this.cargarCuentas();
              this.finalizarGuardado(conductorGuardado, true);
            },
            error: (error: HttpErrorResponse) => {
              this.guardando.set(false);
              this.cerrarFormulario();
              this.cargarConductores();
              this.cargarCuentas();
              this.informarDniDuplicado(error, true);
              this.mensajeError.set(
                'El conductor quedó registrado. No vuelvas a registrarlo. No se creó un nuevo acceso: ' +
                  (error.error?.mensaje || 'revisa los datos de acceso.'),
              );
            },
          });
          return;
        }

        this.finalizarGuardado(conductorGuardado, id === null);
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);
        this.informarDniDuplicado(error);
        this.mensajeError.set(
          error.error?.mensaje ?? error.error?.message ?? 'No fue posible guardar el conductor.',
        );
      },
    });
  }

  errorCampo(campo: string): string {
    const control = this.conductorForm.get(campo);
    if (!control || !control.touched || !control.errors) return '';
    const obligatorios: Record<string, string> = {
      dni: 'Ingresa el DNI del conductor.',
      telefono: 'Ingresa el teléfono del conductor.',
      nombres: 'Ingresa los nombres del conductor.',
      apellidos: 'Ingresa los apellidos del conductor.',
      numeroLicencia: 'Ingresa el número de licencia.',
      categoriaLicencia: 'Selecciona la categoría de licencia.',
      vencimientoLicencia: 'Indica la fecha de vencimiento de la licencia.',
      estado: 'Selecciona el estado del conductor.',
    };
    if (control.hasError('required')) return obligatorios[campo] ?? 'Completa este campo.';
    if (campo === 'correo')
      return 'Ingresa un correo válido, por ejemplo nombre@empresa.com, o déjalo vacío; es opcional.';
    if (campo === 'dni') return 'El DNI debe tener exactamente 8 dígitos.';
    if (campo === 'telefono') return 'El teléfono debe tener 9 dígitos y comenzar con 9.';
    if (campo === 'numeroLicencia')
      return 'Número de licencia: utiliza entre 6 y 20 letras, números o guiones.';
    return 'Revisa el valor ingresado.';
  }
  private informarDniDuplicado(error: HttpErrorResponse, conductorRegistrado = false): void {
    const detalle = error.error?.mensaje ?? error.error?.message ?? '';
    if (error.status !== 409 || !/DNI|documento|cuenta/i.test(detalle)) return;
    const aclaracion = conductorRegistrado
      ? 'El conductor quedó registrado, pero no se creó otra cuenta ni se modificaron sus permisos.'
      : 'Revisa el registro existente o corrige el DNI ingresado.';
    void this.dialogos.informar(
      `Este usuario ya existe: el DNI ingresado ya está asociado a un registro. ${aclaracion}`,
    );
  }

  private finalizarGuardado(conductorGuardado: Conductor, esNuevo: boolean): void {
    this.guardando.set(false);

    const nombreCompleto = `${conductorGuardado.nombres} ${conductorGuardado.apellidos}`;

    const mensaje = esNuevo
      ? `Conductor ${nombreCompleto} registrado correctamente.`
      : `Conductor ${nombreCompleto} actualizado correctamente.`;

    this.cerrarFormulario();
    this.mensajeExito.set(mensaje);
    this.cargarConductores();

    setTimeout(() => {
      this.mensajeExito.set('');
    }, 3000);
  }

  cargarCuentas(): void {
    if (!this.puedeAdministrarCuentas) {
      this.cargandoCuentas.set(false);
      return;
    }
    this.cargandoCuentas.set(true);
    this.errorCuentas.set('');
    this.conductorService.listarCuentas().subscribe({
      next: (cuentas) => {
        this.cuentasChofer.set(cuentas.filter((cuenta) => cuenta.rol === 'CHOFER'));
        this.cargandoCuentas.set(false);
      },
      error: () => {
        this.cargandoCuentas.set(false);
        this.errorCuentas.set('No se pudo verificar la cuenta de acceso.');
      },
    });
  }

  cuentaDe(conductorId: number): CuentaChofer | null {
    const vinculada = this.cuentasChofer().find((cuenta) => cuenta.conductorId === conductorId);
    if (vinculada) return vinculada;
    const dni = this.conductores()
      .find((conductor) => conductor.id === conductorId)
      ?.dni?.trim();
    if (!dni) return null;
    return (
      this.cuentasChofer().find(
        (cuenta) => cuenta.conductorId == null && cuenta.dni?.trim() === dni,
      ) ?? null
    );
  }

  crearAcceso(conductor: Conductor): void {
    if (this.procesandoCuentaId() !== null || this.cargandoCuentas() || this.errorCuentas()) return;
    if (this.cuentaDe(conductor.id)) {
      this.mensajeError.set('El conductor ya tiene acceso. Utiliza Restablecer contraseña.');
      return;
    }

    this.procesandoCuentaId.set(conductor.id);
    this.mensajeError.set('');
    this.conductorService.crearAcceso(conductor.id).subscribe({
      next: (acceso) => {
        this.procesandoCuentaId.set(null);
        this.accesoTemporal.set(acceso);
        this.cargarCuentas();
      },
      error: (error: HttpErrorResponse) => {
        this.procesandoCuentaId.set(null);
        if (error.status === 409) this.cargarCuentas();
        this.mensajeError.set(error.error?.mensaje || 'No fue posible crear la cuenta.');
      },
    });
  }

  async restablecerAcceso(cuenta: CuentaChofer, conductorId: number): Promise<void> {
    if (this.procesandoCuentaId() !== null || !cuenta.activo) return;
    if (
      !(await this.dialogos.confirmar(
        `¿Generar una nueva contraseña temporal para ${cuenta.nombreConductor || cuenta.nombres}?`,
      ))
    )
      return;

    this.procesandoCuentaId.set(conductorId);
    this.mensajeError.set('');
    this.conductorService.restablecerAcceso(cuenta.id).subscribe({
      next: (acceso) => {
        this.procesandoCuentaId.set(null);
        this.accesoTemporal.set(acceso);
      },
      error: (error: HttpErrorResponse) => {
        this.procesandoCuentaId.set(null);
        this.mensajeError.set(error.error?.mensaje || 'No fue posible restablecer la cuenta.');
      },
    });
  }

  cerrarAccesoTemporal(): void {
    this.accesoTemporal.set(null);
  }

  async desactivarConductor(conductor: Conductor): Promise<void> {
    if (conductor.estado === 'INACTIVO') {
      return;
    }

    const confirmar = await this.dialogos.confirmar(
      `¿Deseas desactivar al conductor ${conductor.nombres} ${conductor.apellidos}?`,
    );

    if (!confirmar) {
      return;
    }

    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.conductorService.desactivar(conductor.id).subscribe({
      next: (conductorDesactivado) => {
        this.mensajeExito.set(
          `Conductor ${conductorDesactivado.nombres} desactivado correctamente.`,
        );

        this.cargarConductores();

        setTimeout(() => {
          this.mensajeExito.set('');
        }, 3000);
      },
      error: (error: HttpErrorResponse) => {
        this.mensajeError.set(error.error?.message ?? 'No fue posible desactivar al conductor.');
      },
    });
  }

  volverDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  irVehiculos(): void {
    this.router.navigate(['/vehiculos']);
  }

  cerrarSesion(): void {
    this.sesionService.cerrarSesion();
  }
}
