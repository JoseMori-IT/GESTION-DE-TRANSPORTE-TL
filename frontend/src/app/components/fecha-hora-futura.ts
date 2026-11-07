import { Component, Input, forwardRef, OnDestroy, signal } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  NG_VALIDATORS,
  Validator,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
let siguienteId = 0;
export function proximoMinutoLocal(ahora = new Date()): string {
  const d = new Date(ahora);
  d.setSeconds(0, 0);
  d.setMinutes(d.getMinutes() + 1);
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}T${dos(d.getHours())}:${dos(d.getMinutes())}`;
}
export function fechaFuturaValida(valor: unknown, ahora = new Date()): boolean {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(valor)) return false;
  const fecha = new Date(valor);
  if (!Number.isFinite(fecha.getTime())) return false;
  return (
    fecha.getFullYear() === Number(valor.slice(0, 4)) &&
    fecha.getMonth() + 1 === Number(valor.slice(5, 7)) &&
    fecha.getDate() === Number(valor.slice(8, 10)) &&
    fecha.getHours() === Number(valor.slice(11, 13)) &&
    fecha.getMinutes() === Number(valor.slice(14, 16)) &&
    fecha.getTime() > ahora.getTime()
  );
}
@Component({
  selector: 'app-fecha-hora-futura',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FechaHoraFuturaComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => FechaHoraFuturaComponent),
      multi: true,
    },
  ],
  template: `
    <fieldset [disabled]="deshabilitado">
      <legend>{{ titulo }}</legend>
      <div class="campos">
        <label [for]="id + '-fecha'"
          >Fecha<input
            [id]="id + '-fecha'"
            type="date"
            [value]="fecha"
            [min]="fechaMinima"
            (focus)="actualizarAhora()"
            (input)="cambiar('fecha', $event)"
            (blur)="tocar()"
            [attr.aria-describedby]="id + '-ayuda'"
            [attr.aria-invalid]="error"
        /></label>
        <label [for]="id + '-hora'"
          >Hora<select
            [id]="id + '-hora'"
            [value]="hora"
            (focus)="actualizarAhora()"
            (change)="cambiar('hora', $event)"
            (blur)="tocar()"
            [attr.aria-describedby]="id + '-ayuda'"
            [attr.aria-invalid]="error"
          >
            <option value="" disabled>Selecciona hora</option>
            @if (hora && !horasDisponibles.includes(hora)) {
              <option [value]="hora" disabled>{{ hora }} - no disponible</option>
            }
            @for (opcion of horasDisponibles; track opcion) {
              <option [value]="opcion">{{ opcion }}</option>
            }
          </select></label
        >
      </div>
      <small [id]="id + '-ayuda'" [class.error]="error" aria-live="polite">{{
        error
          ? 'Elige un horario futuro, igual o posterior al requerido en la solicitud.'
          : minima
            ? 'Desde el horario de la solicitud. En días posteriores puedes elegir cualquier hora futura.'
            : 'Si eliges hoy, la hora debe ser posterior a la actual.'
      }}</small>
    </fieldset>
  `,
  styles: [
    `
      :host {
        display: block;
        min-width: 0;
      }
      fieldset {
        padding: 0;
        margin: 0;
        border: 0;
        min-width: 0;
      }
      legend {
        padding: 0;
        margin-bottom: 8px;
        font-size: 12px;
        font-weight: 600;
        color: #294a39;
      }
      .campos {
        display: grid;
        grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
        gap: 10px;
      }
      label {
        display: grid;
        gap: 5px;
        font-size: 11px;
        color: #63705c;
      }
      input,
      select {
        box-sizing: border-box;
        width: 100%;
        min-width: 0;
        height: 44px;
        padding: 0 10px;
        background: #fdfcf7;
        border: 1px solid #cbd2bf;
        border-radius: 5px;
        color: #244535;
        font: inherit;
        font-size: 12px;
      }
      input:focus-visible,
      select:focus-visible {
        outline: 3px solid #819c42;
        outline-offset: 2px;
      }
      input[aria-invalid='true'],
      select[aria-invalid='true'] {
        border-color: #b34535;
      }
      small {
        display: block;
        margin-top: 7px;
        font-size: 10px;
        font-weight: 400;
        line-height: 1.5;
        color: #63705c;
      }
      .error {
        color: #a53729;
      }
    `,
  ],
})
export class FechaHoraFuturaComponent implements ControlValueAccessor, Validator, OnDestroy {
  private limite = '';
  @Input() set minima(v: string | null | undefined) {
    this.limite = v?.slice(0, 16) || '';
    this.validarCambio();
  }
  get minima(): string {
    return this.limite;
  }
  @Input() titulo = 'Fecha y hora requerida';
  readonly id = 'fecha-futura-' + ++siguienteId;
  fecha = '';
  hora = '';
  deshabilitado = false;
  private ahora = signal(new Date());
  private tocado = false;
  private cambio: (v: string) => void = () => {};
  private touched: () => void = () => {};
  private validarCambio: () => void = () => {};
  private temporizador = setInterval(() => this.actualizarAhora(), 30000);
  get hoy() {
    const d = this.ahora();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  get fechaMinima() {
    return this.minima.slice(0, 10) > this.hoy ? this.minima.slice(0, 10) : this.hoy;
  }
  get horasDisponibles(): string[] {
    const ahora = this.ahora();
    if (!this.fecha || this.fecha < this.fechaMinima) return [];
    let inicio = this.fecha === this.hoy ? ahora.getHours() * 60 + ahora.getMinutes() + 1 : 0;
    if (this.fecha === this.minima.slice(0, 10)) {
      inicio = Math.max(
        inicio,
        Number(this.minima.slice(11, 13)) * 60 + Number(this.minima.slice(14, 16)),
      );
    }
    return Array.from({ length: Math.max(0, 1440 - inicio) }, (_, i) => {
      const m = inicio + i;
      return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;
    });
  }
  get error() {
    return (
      this.tocado &&
      !!this.fecha &&
      !!this.hora &&
      (!fechaFuturaValida(this.fecha + 'T' + this.hora, this.ahora()) ||
        (!!this.minima && this.fecha + 'T' + this.hora < this.minima))
    );
  }
  actualizarAhora() {
    this.ahora.set(new Date());
    this.validarCambio();
  }
  cambiar(campo: 'fecha' | 'hora', e: Event) {
    this[campo] = (e.target as HTMLInputElement).value;
    this.tocado = true;
    this.actualizarAhora();
    this.cambio(this.fecha && this.hora ? this.fecha + 'T' + this.hora : '');
    this.touched();
  }
  tocar() {
    this.tocado = true;
    this.touched();
    this.actualizarAhora();
  }
  writeValue(v: string | null) {
    this.fecha = v?.slice(0, 10) ?? '';
    this.hora = v?.slice(11, 16) ?? '';
    this.tocado = false;
  }
  registerOnChange(fn: (v: string) => void) {
    this.cambio = fn;
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(v: boolean) {
    this.deshabilitado = v;
  }
  registerOnValidatorChange(fn: () => void) {
    this.validarCambio = fn;
  }
  validate(c: AbstractControl): ValidationErrors | null {
    return !c.value ||
      (fechaFuturaValida(c.value) && (!this.minima || String(c.value) >= this.minima))
      ? null
      : { fechaPasada: true };
  }
  ngOnDestroy() {
    clearInterval(this.temporizador);
  }
}
