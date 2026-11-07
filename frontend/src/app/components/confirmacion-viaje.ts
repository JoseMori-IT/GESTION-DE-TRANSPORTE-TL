import { Component, ElementRef, afterRenderEffect, input, output, viewChild } from '@angular/core';
export interface ViajeAsignadoConfirmacion {
  codigo: string;
  chofer: string;
  unidad: string;
  recorrido: string;
}
@Component({
  selector: 'app-confirmacion-viaje',
  template: `<dialog
    #ventana
    closedby="none"
    aria-labelledby="viaje-asignado-titulo"
    aria-describedby="viaje-asignado-mensaje"
    (cancel)="$event.preventDefault()"
  >
    <header>
      <div>
        <p>Programación de transporte</p>
        <h2 id="viaje-asignado-titulo">VIAJE ASIGNADO CORRECTAMENTE</h2>
      </div>
      <button
        type="button"
        class="cerrar"
        aria-label="Cerrar confirmación del viaje"
        autofocus
        (click)="cerrar.emit()"
      >
        ×
      </button>
    </header>
    <section>
      <p id="viaje-asignado-mensaje">
        Se registró correctamente el viaje con código <strong>{{ datos().codigo }}</strong
        >.
      </p>
      <dl>
        <div>
          <dt>Asignado a</dt>
          <dd>{{ datos().chofer }}</dd>
        </div>
        <div>
          <dt>Unidad asignada</dt>
          <dd>{{ datos().unidad }}</dd>
        </div>
        <div>
          <dt>Tramo</dt>
          <dd>{{ datos().recorrido }}</dd>
        </div>
      </dl>
    </section>
  </dialog>`,
  styles: [
    `
      dialog {
        box-sizing: border-box;
        width: min(620px, calc(100vw - 32px));
        max-height: 90dvh;
        margin: auto;
        padding: 0;
        border: 1px solid #e4e4eb;
        border-radius: 22px;
        background: #fff;
        color: #1d1d1f;
        box-shadow: 0 24px 80px #0003;
        font-family: 'Palmas Poppins', Arial, sans-serif;
        overflow: auto;
      }
      dialog[open] {
        animation: entrada-viaje 260ms cubic-bezier(0.2, 0.7, 0.2, 1) both;
      }
      dialog[open]::backdrop {
        animation: fondo-viaje 260ms ease-out both;
      }
      @keyframes entrada-viaje {
        from {
          opacity: 0;
          transform: translateY(16px) scale(0.98);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }
      @keyframes fondo-viaje {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        dialog[open],
        dialog[open]::backdrop {
          animation: none;
        }
      }
      dialog::backdrop {
        background: #1d1d1f66;
        backdrop-filter: blur(8px);
      }
      header {
        display: flex;
        align-items: flex-start;
        gap: 18px;
        justify-content: space-between;
        padding: 26px 28px 20px;
        border-bottom: 1px solid #e5e5eb;
      }
      header p {
        margin: 0 0 8px;
        font-size: 12px;
        color: #626875;
      }
      h2 {
        margin: 0;
        font-size: 23px;
        line-height: 1.35;
        font-weight: 600;
        letter-spacing: -0.5px;
      }
      .cerrar {
        flex-shrink: 0;
        display: grid;
        place-items: center;
        width: 44px;
        height: 44px;
        border: 1px solid #e4e4eb;
        border-radius: 50%;
        background: #f5f5f7;
        color: #515154;
        font-size: 26px;
        cursor: pointer;
      }
      .cerrar:focus-visible {
        outline: 3px solid #007aff;
        outline-offset: 3px;
      }
      section {
        padding: 22px 28px 28px;
      }
      section > p {
        margin: 0 0 20px;
        line-height: 1.7;
        font-size: 15px;
        overflow-wrap: anywhere;
      }
      section > p strong {
        color: #0066cc;
        font-weight: 600;
      }
      dl {
        display: grid;
        gap: 12px;
        margin: 0;
      }
      dl > div {
        background: #f7f8fc;
        border: 1px solid #e4e6ee;
        border-radius: 12px;
        padding: 14px 16px;
      }
      dt {
        font-size: 12px;
        color: #626875;
        margin-bottom: 6px;
      }
      dd {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        line-height: 1.65;
        overflow-wrap: anywhere;
      }
      @media (max-width: 500px) {
        header {
          padding: 22px 20px 18px;
          gap: 12px;
        }
        section {
          padding: 20px;
        }
        h2 {
          font-size: 21px;
        }
        dd {
          font-size: 15px;
        }
      }
    `,
  ],
})
export class ConfirmacionViajeComponent {
  readonly datos = input.required<ViajeAsignadoConfirmacion>();
  readonly cerrar = output<void>();
  private readonly ventana = viewChild.required<ElementRef<HTMLDialogElement>>('ventana');
  constructor() {
    afterRenderEffect(() => {
      const dialog = this.ventana().nativeElement;
      if (!dialog.open) {
        if (typeof dialog.showModal === 'function') dialog.showModal();
        else dialog.setAttribute('open', '');
      }
    });
  }
}
