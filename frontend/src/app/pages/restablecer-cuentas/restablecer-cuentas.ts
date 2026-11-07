import { Component, ElementRef, inject, signal, OnInit, viewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface Cuenta {
  id: number;
  nombres: string;
  correo: string | null;
  rol: string;
  activo: boolean;
  conductorId: number | null;
}
interface Temporal {
  usuarioId: number;
  identificador: string;
  contrasenaTemporal: string;
  venceEn: string;
}

@Component({
  selector: 'app-restablecer-cuentas',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="content">
      <section>
        <img src="assets/branding/logopalmas.webp" alt="Grupo Palmas" width="150" />
        <h1>Restablecer acceso</h1>
        <p>
          Verifica la identidad del usuario antes de restablecer su contraseña. Sus sesiones se
          cerrarán.
        </p>
        <form [formGroup]="form" (ngSubmit)="restablecer()">
          <label for="cuenta">Cuenta activa</label>
          <select id="cuenta" formControlName="usuarioId">
            <option value="">Selecciona una cuenta</option>
            @for (cuenta of cuentas(); track cuenta.id) {
              <option [value]="cuenta.id">
                {{ cuenta.nombres }} · {{ cuenta.correo || 'Conductor ' + cuenta.conductorId }} ·
                {{ cuenta.rol }}
              </option>
            }
          </select>
          <label for="motivo">Motivo y referencia de verificación de identidad</label>
          <textarea id="motivo" formControlName="motivo" maxlength="500" rows="3"></textarea>
          <small
            >Registra solo la referencia necesaria. No incluyas contraseñas ni documentos
            completos.</small
          >
          <button type="submit" [disabled]="procesando() || cargando()">
            {{ procesando() ? 'Restableciendo...' : 'Generar contraseña temporal' }}
          </button>
        </form>
        @if (mensaje()) {
          <p role="alert">{{ mensaje() }}</p>
        }
        @if (temporal(); as resultado) {
          <div class="temporal" role="status">
            <h2>Acceso temporal generado</h2>
            <p>
              Usuario: <strong>{{ resultado.identificador }}</strong>
            </p>
            <label for="claveTemporal">Contraseña temporal</label>
            <input
              id="claveTemporal"
              readonly
              [value]="resultado.contrasenaTemporal"
              autocomplete="off"
            />
            <p>Vence: {{ resultado.venceEn.replace('T', ' ') }}</p>
            <p>
              Entrégala por un canal autorizado tras verificar la identidad. El usuario debe
              cambiarla al ingresar.
            </p>
            <button type="button" (click)="temporal.set(null)">Ocultar contraseña</button>
          </div>
        }
        <a routerLink="/dashboard">Volver al resumen</a>
      </section>
    </main>
    <dialog
      #confirmacion
      class="confirmation"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-description"
      (cancel)="cancelarRestablecimiento()"
    >
      <span class="dialog-label">Gestión de accesos</span>
      <h2 id="confirm-title">¿Restablecer el acceso?</h2>
      <p id="confirm-description">
        Se generará una contraseña temporal para <strong>{{ pendiente()?.nombre }}</strong> y se
        cerrarán sus sesiones actuales.
      </p>
      <p class="dialog-note">La persona deberá cambiar la contraseña temporal al ingresar.</p>
      <div class="dialog-actions">
        <button class="secondary" type="button" autofocus (click)="cancelarRestablecimiento()">
          Cancelar
        </button>
        <button type="button" (click)="confirmarRestablecimiento()">Sí, restablecer acceso</button>
      </div>
    </dialog>
  `,
  styles: [
    `
      .confirmation {
        width: min(480px, calc(100vw - 32px));
        max-height: calc(100dvh - 32px);
        padding: 28px;
        color: #193d2c;
        background: #f8f7f1;
        border: 1px solid #daddce;
        border-radius: 6px;
        overflow-y: auto;
        font: inherit;
        box-shadow: 0 24px 80px #09271940;
      }
      .confirmation::backdrop {
        background: #09271980;
        backdrop-filter: blur(3px);
      }
      .confirmation h2 {
        margin: 12px 0 14px;
        font-size: 23px;
        line-height: 1.35;
      }
      .dialog-label {
        color: #63705c;
        font-size: 12px;
      }
      .dialog-note {
        padding: 12px;
        background: #e9eddf;
        border-radius: 4px;
        font-size: 12px;
      }
      .dialog-actions {
        display: flex;
        justify-content: flex-end;
        flex-wrap: wrap;
        gap: 10px;
        margin-top: 24px;
      }
      .dialog-actions .secondary {
        margin-top: 0;
      }
      @media (max-width: 480px) {
        .confirmation {
          padding: 24px 20px;
        }
        .dialog-actions {
          flex-direction: column-reverse;
        }
        .dialog-actions button {
          width: 100%;
        }
      }
      @media (prefers-reduced-motion: no-preference) {
        .confirmation[open] {
          animation: confirm-enter 180ms ease-out;
        }
      }
      @keyframes confirm-enter {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
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
export class RestablecerCuentasPage implements OnInit {
  private readonly confirmacion = viewChild.required<ElementRef<HTMLDialogElement>>('confirmacion');
  readonly pendiente = signal<{ id: number; nombre: string; motivo: string } | null>(null);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  readonly cuentas = signal<Cuenta[]>([]);
  readonly temporal = signal<Temporal | null>(null);
  readonly mensaje = signal('');
  readonly cargando = signal(true);
  readonly procesando = signal(false);
  readonly form = this.fb.nonNullable.group({
    usuarioId: ['', Validators.required],
    motivo: ['', [Validators.required, Validators.maxLength(500)]],
  });
  ngOnInit(): void {
    this.http.get<Cuenta[]>('/api/usuarios').subscribe({
      next: (cuentas) => {
        const rolSesion = sessionStorage.getItem('rol');
        this.cuentas.set(
          cuentas.filter(
            (c) =>
              c.activo &&
              c.rol !== 'MASTER' &&
              c.rol !== 'ADMINISTRADOR' &&
              (rolSesion !== 'COORDINADOR' || c.rol !== 'COORDINADOR'),
          ),
        );
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.mensaje.set('No fue posible consultar las cuentas.');
      },
    });
  }
  restablecer(): void {
    if (this.procesando()) return;
    this.temporal.set(null);
    this.mensaje.set('');
    const datos = this.form.getRawValue();
    const cuenta = this.cuentas().find((c) => c.id === Number(datos.usuarioId));
    if (this.form.invalid || !cuenta || !datos.motivo.trim()) {
      this.form.markAllAsTouched();
      this.mensaje.set('Selecciona la cuenta e indica el motivo.');
      return;
    }
    this.pendiente.set({ id: cuenta.id, nombre: cuenta.nombres, motivo: datos.motivo.trim() });
    this.confirmacion().nativeElement.showModal();
  }

  cancelarRestablecimiento(): void {
    this.confirmacion().nativeElement.close();
    this.pendiente.set(null);
  }

  confirmarRestablecimiento(): void {
    const pendiente = this.pendiente();
    if (!pendiente || this.procesando()) return;
    this.cancelarRestablecimiento();
    this.procesando.set(true);
    this.http
      .post<Temporal>('/api/usuarios/' + pendiente.id + '/restablecer-contrasena', {
        motivo: pendiente.motivo,
      })
      .subscribe({
        next: (resultado) => {
          this.temporal.set(resultado);
          this.procesando.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.procesando.set(false);
          this.mensaje.set(error.error?.mensaje ?? 'No se pudo restablecer el acceso.');
        },
      });
  }
}
