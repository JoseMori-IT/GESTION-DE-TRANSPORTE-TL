import { Injectable, inject, signal } from '@angular/core';
import { Router, NavigationStart } from '@angular/router';
export interface DialogoApp {
  mensaje: string;
  informativo: boolean;
}
@Injectable({ providedIn: 'root' })
export class DialogosService {
  readonly actual = signal<DialogoApp | null>(null);
  private resolverPendiente: ((valor: boolean) => void) | null = null;
  constructor() {
    inject(Router, { optional: true })?.events.subscribe((e) => {
      if (e instanceof NavigationStart) this.resolver(false);
    });
  }
  confirmar(mensaje: string): Promise<boolean> {
    return this.abrir(mensaje, false);
  }
  async informar(mensaje: string): Promise<void> {
    await this.abrir(mensaje, true);
  }
  private abrir(mensaje: string, informativo: boolean): Promise<boolean> {
    if (this.actual()) return Promise.resolve(false);
    return new Promise((resolve) => {
      this.resolverPendiente = resolve;
      this.actual.set({ mensaje, informativo });
    });
  }
  resolver(valor: boolean): void {
    const fn = this.resolverPendiente;
    this.resolverPendiente = null;
    this.actual.set(null);
    fn?.(valor);
  }
}
