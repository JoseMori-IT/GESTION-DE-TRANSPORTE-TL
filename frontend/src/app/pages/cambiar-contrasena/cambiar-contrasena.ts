import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-cambiar-contrasena',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="password-page content">
      <section class="password-card">
        <img src="assets/branding/logopalmas.webp" alt="Grupo Palmas" width="150" />
        <h1>Cambiar contraseña</h1>
        @if (terminado()) {
          <p role="status">Tu contraseña se cambió y las sesiones anteriores se cerraron.</p>
          <a routerLink="/login">Iniciar sesión con la nueva contraseña</a>
        } @else {
          @if (obligatorio) {
            <p>Debes cambiar tu contraseña inicial o temporal antes de continuar.</p>
          }
          <form [formGroup]="form" (ngSubmit)="guardar()">
            <label for="actual">Contraseña actual o temporal</label>
            <input
              id="actual"
              type="password"
              autocomplete="current-password"
              formControlName="contrasenaActual"
              maxlength="100"
            />
            <label for="nueva">Nueva contraseña</label>
            <input
              id="nueva"
              type="password"
              autocomplete="new-password"
              formControlName="contrasenaNueva"
              maxlength="64"
              aria-describedby="politica"
            />
            <small id="politica">Entre 12 y 64 caracteres. Puedes usar una frase larga.</small>
            <label for="confirmacion">Confirmar nueva contraseña</label>
            <input
              id="confirmacion"
              type="password"
              autocomplete="new-password"
              formControlName="confirmacion"
              maxlength="64"
            />
            @if (mensaje()) {
              <p class="error" role="alert">{{ mensaje() }}</p>
            }
            <button type="submit" [disabled]="guardando()">
              {{ guardando() ? 'Guardando...' : 'Guardar nueva contraseña' }}
            </button>
          </form>
          @if (!obligatorio) {
            <a [routerLink]="rol === 'CHOFER' ? '/chofer' : '/dashboard'">Volver</a>
          }
        }
      </section>
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        color: #193d2c;
        font-family: 'Palmas Poppins', sans-serif;
        font-size: 14px;
      }
      * {
        box-sizing: border-box;
      }
      main,
      .password-page {
        min-height: 100dvh;
        display: grid;
        align-content: start;
        justify-items: center;
        padding: 40px 32px;
        background: #f0f0e7;
      }
      section,
      .password-card {
        width: 100%;
        max-width: 650px;
        margin: 0;
        padding: 32px;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 6px;
      }
      .password-card {
        max-width: 520px;
      }
      img {
        display: block;
        width: 130px;
        height: 68px;
        object-fit: contain;
        object-position: left center;
        margin-bottom: 24px;
      }
      h1 {
        margin: 0 0 20px;
        color: #153e2c;
        font-size: 27px;
        font-weight: 600;
        letter-spacing: -0.035em;
      }
      h2 {
        font-size: 19px;
        font-weight: 600;
      }
      p,
      small {
        line-height: 1.7;
      }
      p {
        color: #52634d;
      }
      small {
        font-size: 12px;
        color: #63705c;
      }
      form {
        display: grid;
        gap: 12px;
      }
      label {
        font-size: 13px;
        font-weight: 600;
        margin-top: 5px;
      }
      input,
      select,
      textarea {
        width: 100%;
        min-width: 0;
        padding: 12px;
        border: 1px solid #cbd2bf;
        border-radius: 4px;
        background: #fdfcf7;
        color: #193d2c;
        font: inherit;
      }
      input:focus,
      select:focus,
      textarea:focus {
        outline: 2px solid #819c42;
        outline-offset: 1px;
      }
      button {
        padding: 13px 16px;
        border: 0;
        border-radius: 4px;
        background: #204d36;
        color: white;
        font: inherit;
        font-weight: 600;
        cursor: pointer;
      }
      button:hover:not(:disabled) {
        background: #153e2c;
      }
      button:disabled {
        opacity: 0.6;
        cursor: wait;
      }
      button:focus-visible,
      a:focus-visible {
        outline: 3px solid #819c42;
        outline-offset: 3px;
      }
      .secondary {
        margin-top: 16px;
        background: #e9eddf;
        color: #193d2c;
      }
      a {
        display: block;
        margin-top: 20px;
        color: #365b32;
      }
      .error {
        color: #a82920;
      }
      .temporal {
        margin-top: 24px;
        padding: 20px;
        border: 1px solid #cbd2bf;
        background: #e9eddf;
        border-radius: 5px;
        overflow-wrap: anywhere;
      }
      @media (max-width: 600px) {
        main,
        .password-page {
          padding: 24px 16px;
        }
        section,
        .password-card {
          padding: 24px 20px;
        }
        h1 {
          font-size: 24px;
        }
      }
    `,
  ],
})
export class CambiarContrasenaPage {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  readonly sesion = inject(SesionService);
  readonly obligatorio = sessionStorage.getItem('cambioContrasenaRequerido') === 'true';
  readonly rol = sessionStorage.getItem('rol');
  readonly guardando = signal(false);
  readonly terminado = signal(false);
  readonly mensaje = signal('');
  readonly form = this.fb.nonNullable.group({
    contrasenaActual: ['', [Validators.required, Validators.maxLength(100)]],
    contrasenaNueva: [
      '',
      [Validators.required, Validators.minLength(12), Validators.maxLength(64)],
    ],
    confirmacion: ['', Validators.required],
  });

  guardar(): void {
    if (this.guardando()) return;
    this.mensaje.set('');
    const datos = this.form.getRawValue();
    if (this.form.invalid || !datos.contrasenaNueva.trim()) {
      this.form.markAllAsTouched();
      this.mensaje.set(
        'Completa los campos. La nueva contraseña debe tener entre 12 y 64 caracteres.',
      );
      return;
    }
    if (datos.contrasenaNueva !== datos.confirmacion) {
      this.mensaje.set('La confirmación no coincide con la nueva contraseña.');
      return;
    }
    if (new TextEncoder().encode(datos.contrasenaNueva).length > 72) {
      this.mensaje.set('La nueva contraseña supera el tamaño permitido. Usa menos caracteres.');
      return;
    }
    this.guardando.set(true);
    this.http
      .post<void>('/api/auth/cambiar-contrasena', {
        contrasenaActual: datos.contrasenaActual,
        contrasenaNueva: datos.contrasenaNueva,
      })
      .subscribe({
        next: () => {
          this.form.reset();
          for (const clave of [
            'token',
            'rol',
            'usuarioId',
            'nombres',
            'correo',
            'conductorId',
            'usuario',
            'cambioContrasenaRequerido',
          ]) {
            sessionStorage.removeItem(clave);
          }
          this.guardando.set(false);
          this.terminado.set(true);
        },
        error: (error: HttpErrorResponse) => {
          this.guardando.set(false);
          this.mensaje.set(error.error?.mensaje ?? 'No se pudo cambiar la contraseña.');
        },
      });
  }
}
