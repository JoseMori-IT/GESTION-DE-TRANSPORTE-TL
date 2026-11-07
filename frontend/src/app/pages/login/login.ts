import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AccesoOfflineService } from '../../services/acceso-offline.service';

interface LoginResponse {
  id: number;
  nombres: string;
  correo: string | null;

  rol: 'MASTER' | 'COORDINADOR' | 'ADMINISTRADOR' | 'OPERADOR' | 'CONSULTA' | 'CHOFER';

  conductorId: number | null;
  token: string;
  mensaje: string;
  cambioContrasenaRequerido: boolean;
}

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  readonly accesoOffline = inject(AccesoOfflineService);
  readonly fraseLocal = signal('');
  async abrirViajeGuardado(): Promise<void> {
    if (this.cargando()) return;
    this.cargando.set(true);
    try {
      await this.accesoOffline.reanudarAutomatico();
      await this.router.navigate(['/chofer']);
    } catch (e) {
      this.esError.set(true);
      this.mensaje.set(e instanceof Error ? e.message : 'No se pudo abrir el viaje guardado.');
    } finally {
      this.cargando.set(false);
    }
  }
  async desbloquearLocal(): Promise<void> {
    if (this.cargando()) return;
    this.cargando.set(true);
    try {
      await this.accesoOffline.desbloquear(this.fraseLocal());
      this.fraseLocal.set('');
      await this.router.navigate(['/chofer']);
    } catch (e) {
      this.esError.set(true);
      this.mensaje.set(e instanceof Error ? e.message : 'No se pudo desbloquear.');
    } finally {
      this.cargando.set(false);
    }
  }
  private readonly destroyRef = inject(DestroyRef);
  readonly accesoCorrecto = signal(false);
  readonly mostrarContrasena = signal(false);
  readonly cargando = signal(false);
  readonly mensaje = signal('');
  readonly esError = signal(false);

  readonly loginForm;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {
    this.loginForm = this.formBuilder.nonNullable.group({
      identificador: [
        '',
        [Validators.required, Validators.pattern(/^(?:[A-Za-z0-9._%+-]+@palmas\.com\.pe|\d{8})$/i)],
      ],

      contrasena: ['', [Validators.required]],
    });
  }

  iniciarSesion(): void {
    if (this.cargando()) return;
    this.mensaje.set('');

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.esError.set(true);

      this.mensaje.set('Completa correctamente los campos.');

      return;
    }

    this.cargando.set(true);

    this.http.post<LoginResponse>('/api/auth/login', this.loginForm.getRawValue()).subscribe({
      next: (respuesta) => {
        for (const clave of Object.keys(sessionStorage)) {
          if (clave.startsWith('transporte-aviso-')) sessionStorage.removeItem(clave);
        }
        this.accesoOffline.bloquear();
        sessionStorage.removeItem('accesoOffline');
        sessionStorage.removeItem('modoAccesoLocal');
        sessionStorage.setItem('token', respuesta.token);

        sessionStorage.setItem('rol', respuesta.rol);

        sessionStorage.setItem('usuarioId', respuesta.id.toString());

        sessionStorage.setItem('nombres', respuesta.nombres);

        if (respuesta.correo) {
          sessionStorage.setItem('correo', respuesta.correo);
        } else {
          sessionStorage.removeItem('correo');
        }

        if (respuesta.conductorId !== null) {
          sessionStorage.setItem('conductorId', respuesta.conductorId.toString());
        } else {
          sessionStorage.removeItem('conductorId');
        }

        this.esError.set(false);
        this.accesoCorrecto.set(true);

        sessionStorage.setItem(
          'cambioContrasenaRequerido',
          String(respuesta.cambioContrasenaRequerido === true),
        );
        const destino = respuesta.cambioContrasenaRequerido
          ? '/cambiar-contrasena'
          : respuesta.rol === 'CHOFER'
            ? '/chofer'
            : '/dashboard';
        const pausa = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 480;
        const temporizador = window.setTimeout(() => {
          if (this.destroyRef.destroyed) return;
          void this.router
            .navigate([destino])
            .then((navego) => {
              if (!navego && !this.destroyRef.destroyed) this.restaurarAcceso();
            })
            .catch(() => {
              if (!this.destroyRef.destroyed) this.restaurarAcceso();
            });
        }, pausa);
        this.destroyRef.onDestroy(() => window.clearTimeout(temporizador));
      },

      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        this.esError.set(true);

        const detalleBackend = error.error?.mensaje ?? error.error?.message ?? error.error?.detail;

        if (detalleBackend) {
          this.mensaje.set(detalleBackend);

          return;
        }

        if (error.status === 0) {
          this.mensaje.set('No se pudo conectar ' + 'con el servidor.');

          return;
        }

        this.mensaje.set(`No fue posible iniciar sesión. ` + `Error HTTP ${error.status}.`);
      },
    });
  }

  private restaurarAcceso(): void {
    this.accesoCorrecto.set(false);
    this.cargando.set(false);
    this.esError.set(true);
    this.mensaje.set('No se pudo abrir el módulo. Intenta iniciar sesión nuevamente.');
  }
}
