import {
  Component,
  Input,
  Output,
  EventEmitter,
  ViewChild,
  ElementRef,
  OnChanges,
  AfterViewInit,
  OnDestroy,
  signal,
} from '@angular/core';
@Component({
  selector: 'app-ajustar-foto-vehiculo',
  template: `
    <canvas
      #lienzo
      width="1200"
      height="750"
      aria-label="Vista previa del encuadre de la unidad"
      (pointerdown)="iniciar($event)"
      (pointermove)="mover($event)"
      (pointerup)="terminar()"
      (pointercancel)="terminar()"
    ></canvas>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
    } @else {
      <p>Arrastra la foto o utiliza los controles para acomodar el vehículo.</p>
      <div class="ajustes">
        <label
          >Zoom<input
            type="range"
            min="1"
            max="3"
            step="0.05"
            [value]="zoom"
            (input)="ajustar('zoom', $event)" /></label
        ><label
          >Horizontal<input
            type="range"
            min="-100"
            max="100"
            [value]="x"
            (input)="ajustar('x', $event)" /></label
        ><label
          >Vertical<input
            type="range"
            min="-100"
            max="100"
            [value]="y"
            (input)="ajustar('y', $event)"
        /></label>
      </div>
      <button type="button" (click)="restablecer()">Restablecer encuadre</button
      ><small>El encuadre mostrado se guardará con la unidad.</small>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
        min-width: 0;
      }
      canvas {
        display: block;
        width: 100%;
        height: auto;
        aspect-ratio: 8/5;
        touch-action: none;
        background: #f5f5f7;
        border: 1px solid #e2e3e9;
        border-radius: 12px;
        box-sizing: border-box;
        cursor: grab;
      }
      canvas:active {
        cursor: grabbing;
      }
      .ajustes {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 16px;
      }
      label {
        display: grid;
        gap: 8px;
        font-size: 13px;
        font-weight: 500;
      }
      input {
        width: 100%;
        min-height: 30px;
        accent-color: #007aff;
        padding: 0;
        box-sizing: border-box;
      }
      p,
      small {
        color: #626875;
        font-size: 12px;
        line-height: 1.6;
      }
      small {
        display: block;
        margin-top: 10px;
      }
      button {
        font: inherit;
        font-size: 13px;
        padding: 10px 14px;
        border: 1px solid #d8d9df;
        border-radius: 10px;
        background: #fff;
        color: #363b45;
        cursor: pointer;
        min-height: 44px;
      }
      @media (max-width: 480px) {
        .ajustes {
          grid-template-columns: 1fr;
          gap: 10px;
        }
      }
    `,
  ],
})
export class AjustarFotoVehiculo implements OnChanges, AfterViewInit, OnDestroy {
  @Input({ required: true }) archivo!: File;
  @Output() ajustada = new EventEmitter<File>();
  @Output() procesando = new EventEmitter<boolean>();
  @ViewChild('lienzo') lienzo!: ElementRef<HTMLCanvasElement>;
  readonly error = signal('');
  zoom = 1;
  x = 0;
  y = 0;
  private imagen: HTMLImageElement | null = null;
  private url = '';
  private preparada = false;
  private serial = 0;
  private destruida = false;
  private arrastre: { x: number; y: number; prevX: number; prevY: number } | null = null;
  ngAfterViewInit() {
    this.preparada = true;
    this.cargar();
  }
  ngOnChanges() {
    if (this.preparada) this.cargar();
  }
  private cargar() {
    this.serial++;
    this.procesando.emit(true);
    this.error.set('');
    if (this.url) URL.revokeObjectURL(this.url);
    this.url = URL.createObjectURL(this.archivo);
    const img = new Image();
    this.imagen = img;
    img.onload = () => {
      if (this.destruida || img !== this.imagen) return;
      this.zoom = 1;
      this.x = 0;
      this.y = 0;
      this.dibujar();
    };
    img.onerror = () => {
      if (img === this.imagen && !this.destruida) {
        this.error.set('No se pudo leer esta imagen. Selecciona otra fotografía.');
        this.procesando.emit(false);
      }
    };
    img.src = this.url;
  }
  ajustar(campo: 'zoom' | 'x' | 'y', e: Event) {
    this[campo] = Number((e.target as HTMLInputElement).value);
    this.dibujar();
  }
  restablecer() {
    this.zoom = 1;
    this.x = 0;
    this.y = 0;
    this.dibujar();
  }
  iniciar(e: PointerEvent) {
    this.lienzo.nativeElement.setPointerCapture(e.pointerId);
    this.arrastre = { x: e.clientX, y: e.clientY, prevX: this.x, prevY: this.y };
  }
  mover(e: PointerEvent) {
    if (!this.arrastre) return;
    const rect = this.lienzo.nativeElement.getBoundingClientRect();
    this.x = Math.max(
      -100,
      Math.min(100, this.arrastre.prevX + ((e.clientX - this.arrastre.x) / rect.width) * 200),
    );
    this.y = Math.max(
      -100,
      Math.min(100, this.arrastre.prevY + ((e.clientY - this.arrastre.y) / rect.height) * 200),
    );
    this.dibujar();
  }
  terminar() {
    this.arrastre = null;
  }
  private dibujar() {
    const img = this.imagen;
    if (!img?.naturalWidth) return;
    const canvas = this.lienzo.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const escala =
      Math.min(canvas.width / img.naturalWidth, canvas.height / img.naturalHeight) * this.zoom;
    const w = img.naturalWidth * escala,
      h = img.naturalHeight * escala;
    ctx.fillStyle = '#f5f5f7';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(
      img,
      (canvas.width - w) / 2 + ((this.x / 100) * Math.abs(canvas.width - w)) / 2,
      (canvas.height - h) / 2 + ((this.y / 100) * Math.abs(canvas.height - h)) / 2,
      w,
      h,
    );
    const n = ++this.serial;
    this.procesando.emit(true);
    canvas.toBlob(
      (blob) => {
        if (n !== this.serial || this.destruida) return;
        if (!blob) {
          this.error.set('No se pudo preparar la fotografía. Selecciona otra.');
          this.procesando.emit(false);
          return;
        }
        this.ajustada.emit(new File([blob], 'vehiculo.jpg', { type: 'image/jpeg' }));
        this.procesando.emit(false);
      },
      'image/jpeg',
      0.92,
    );
  }
  ngOnDestroy() {
    this.destruida = true;
    this.serial++;
    if (this.url) URL.revokeObjectURL(this.url);
  }
}
