import { Component, Input } from '@angular/core';
export interface BarraResumen {
  nombre: string;
  valor: number;
  color: string;
}
@Component({
  selector: 'app-grafico-barras',
  template: `
    <div class="grafico" [attr.aria-label]="titulo">
      <div class="escala" aria-hidden="true">
        @for (t of marcas; track $index) {
          <span>{{ t }}</span>
        }
      </div>
      <div class="zona">
        <div class="rejilla" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div>
        <ol>
          @for (b of datos; track b.nombre) {
            <li>
              <div class="columna">
                <div
                  class="barra"
                  [style.height.%]="(b.valor * 100) / maximo"
                  [style.background]="b.color"
                >
                  <strong>{{ b.valor }}</strong>
                </div>
              </div>
              <span class="nombre"
                ><i [style.background]="b.color" aria-hidden="true"></i>{{ b.nombre }}</span
              >
            </li>
          }
        </ol>
      </div>
    </div>
    @if (!total) {
      <p class="sin-datos">No hay registros para estas categorías.</p>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        min-width: 0;
        font-family: 'Palmas Poppins', sans-serif;
      }
      .grafico {
        display: flex;
        gap: 12px;
        padding-top: 20px;
      }
      .escala {
        height: 200px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        flex: 0 0 24px;
        text-align: right;
        color: #737985;
        font-size: 10px;
      }
      .zona {
        position: relative;
        flex: 1;
        min-width: 0;
      }
      .rejilla {
        position: absolute;
        inset: 0 0 auto;
        height: 200px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        pointer-events: none;
      }
      .rejilla i {
        border-top: 1px dashed #e4e7ed;
        width: 100%;
      }
      ol {
        position: relative;
        display: flex;
        list-style: none;
        padding: 0;
        margin: 0;
        gap: 14px;
      }
      li {
        flex: 1;
        min-width: 0;
        text-align: center;
      }
      .columna {
        height: 200px;
        display: flex;
        align-items: flex-end;
        justify-content: center;
        border-bottom: 1px solid #dce0e8;
      }
      .barra {
        position: relative;
        width: min(100%, 68px);
        border-radius: 6px 6px 0 0;
        transition: height 0.5s ease;
      }
      .barra strong {
        position: absolute;
        top: -24px;
        left: 0;
        right: 0;
        color: #1d1d1f;
        font-size: 14px;
        font-weight: 600;
      }
      .nombre {
        display: block;
        margin-top: 12px;
        font-size: 11px;
        line-height: 1.6;
        color: #626875;
        overflow-wrap: anywhere;
      }
      .nombre i {
        display: inline-block;
        width: 7px;
        height: 7px;
        border-radius: 2px;
        margin-right: 6px;
      }
      .sin-datos {
        font-size: 11px;
        color: #737985;
        margin: 14px 0 0;
      }
      @media (max-width: 500px) {
        ol {
          gap: 8px;
        }
        .nombre {
          font-size: 10px;
        }
        .barra {
          max-width: 45px;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        .barra {
          transition: none;
        }
      }
    `,
  ],
})
export class GraficoBarrasComponent {
  @Input() titulo = 'Gráfico de barras';
  @Input() datos: BarraResumen[] = [];
  get total() {
    return this.datos.reduce((n, b) => n + b.valor, 0);
  }
  get maximo() {
    const n = Math.max(1, ...this.datos.map((b) => b.valor));
    const paso = Math.max(1, Math.ceil(n / 4));
    return paso * 4;
  }
  get marcas() {
    return [this.maximo, (this.maximo * 3) / 4, this.maximo / 2, this.maximo / 4, 0];
  }
}
