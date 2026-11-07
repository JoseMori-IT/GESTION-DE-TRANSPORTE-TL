import { Component, ElementRef, afterRenderEffect, inject, viewChild } from '@angular/core';
import { DialogosService } from '../services/dialogos.service';
@Component({
  selector: 'app-dialogo',
  template: `
    <dialog
      #ventana
      aria-labelledby="dialogo-titulo"
      aria-describedby="dialogo-mensaje"
      (cancel)="$event.preventDefault(); dialogos.resolver(false)"
    >
      @if (dialogos.actual(); as d) {
        <div class="contenido">
          <p class="contexto">Gestión de transporte</p>
          <h2 id="dialogo-titulo">{{ d.informativo ? 'Aviso' : 'Confirmar acción' }}</h2>
          <p id="dialogo-mensaje">{{ d.mensaje }}</p>
        </div>
        <footer>
          @if (!d.informativo) {
            <button type="button" class="volver" autofocus (click)="dialogos.resolver(false)">
              {{ d.informativo ? '' : 'No' }}
            </button>
          }
          <button type="button" class="confirmar" (click)="dialogos.resolver(true)">
            {{ d.informativo ? 'Entendido' : 'Sí' }}
          </button>
        </footer>
      }
    </dialog>
  `,
  styles: [
    `
      dialog {
        box-sizing: border-box;
        width: min(460px, calc(100vw - 32px));
        max-height: 85dvh;
        margin: auto;
        padding: 0;
        border: 1px solid #ffffffaa;
        border-radius: 22px;
        background: #fff;
        box-shadow: 0 28px 90px #00000030;
        color: #1d1d1f;
        font-family: 'Palmas Poppins', sans-serif;
        overflow: auto;
      }
      dialog::backdrop {
        background: rgba(29, 29, 31, 0.4);
        backdrop-filter: blur(12px);
      }
      .contenido {
        padding: 30px 30px 24px;
      }
      .contexto {
        margin: 0 0 6px;
        color: #86868b;
        font-size: 11px;
      }
      h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        letter-spacing: -0.5px;
      }
      #dialogo-mensaje {
        margin: 14px 0 0;
        color: #515154;
        font-size: 13px;
        line-height: 1.8;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
      }
      footer {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        padding: 18px 24px;
        background: #f5f5f7;
        border-top: 1px solid #e4e4e9;
      }
      button {
        min-height: 43px;
        min-width: 110px;
        padding: 11px 18px;
        border-radius: 10px;
        font:
          600 12px 'Palmas Poppins',
          sans-serif;
        cursor: pointer;
      }
      .volver {
        background: #fff;
        border: 1px solid #d6d6dc;
        color: #515154;
      }
      .confirmar {
        background: #0071e3;
        border: 1px solid #0071e3;
        color: #fff;
      }
      button:focus-visible {
        outline: 3px solid #0071e366;
        outline-offset: 3px;
      }
      @media (max-width: 500px) {
        .contenido {
          padding: 24px;
        }
        footer {
          padding: 16px 20px;
        }
        button {
          flex: 1;
        }
      }
    `,
  ],
})
export class DialogoAppComponent {
  readonly dialogos = inject(DialogosService);
  private ventana = viewChild.required<ElementRef<HTMLDialogElement>>('ventana');
  constructor() {
    afterRenderEffect(() => {
      const d = this.dialogos.actual(),
        el = this.ventana().nativeElement;
      if (d && !el.open) {
        if (typeof el.showModal === 'function') el.showModal();
        else el.setAttribute('open', '');
      } else if (!d && el.open) {
        if (typeof el.close === 'function') el.close();
        else el.removeAttribute('open');
      }
    });
  }
}
