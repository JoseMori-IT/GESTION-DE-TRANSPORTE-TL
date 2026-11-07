import { Directive, ElementRef, HostListener, Input, inject } from '@angular/core';

import { NgControl } from '@angular/forms';

@Directive({
  selector: 'input[appSoloDigitos]',
  standalone: true,
})
export class SoloDigitosDirective {
  private readonly elemento = inject(ElementRef<HTMLInputElement>);

  private readonly ngControl = inject(NgControl, {
    optional: true,
    self: true,
  });

  @Input()
  limiteDigitos = 8;

  @HostListener('keydown', ['$event'])
  bloquearCaracteres(evento: KeyboardEvent): void {
    if (evento.ctrlKey || evento.metaKey || evento.altKey) {
      return;
    }

    const teclasPermitidas = [
      'Backspace',
      'Delete',
      'Tab',
      'Escape',
      'Enter',
      'ArrowLeft',
      'ArrowRight',
      'Home',
      'End',
    ];

    if (teclasPermitidas.includes(evento.key)) {
      return;
    }

    if (!/^\d$/.test(evento.key)) {
      evento.preventDefault();
    }
  }

  @HostListener('input')
  limpiarContenido(): void {
    const input = this.elemento.nativeElement;

    const limite = Number.isFinite(this.limiteDigitos) ? Math.max(0, this.limiteDigitos) : 8;

    const valorLimpio = input.value.replace(/\D/g, '').slice(0, limite);

    if (input.value === valorLimpio) {
      return;
    }

    input.value = valorLimpio;

    this.ngControl?.control?.setValue(valorLimpio);
  }
}
