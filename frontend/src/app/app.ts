import { SesionActivaService } from './services/sesion-activa.service';
import { DialogoAppComponent } from './components/dialogo-app';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { MenuPrincipal } from './components/menu-principal/menu-principal';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MenuPrincipal, DialogoAppComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly sesionActiva = inject(SesionActivaService);
  private readonly router = inject(Router);
  private readonly ruta = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  mostrarMenu(): boolean {
    const ruta = this.ruta().split('?')[0].split('#')[0];
    return (
      (!!sessionStorage.getItem('token') || sessionStorage.getItem('accesoOffline') === 'true') &&
      !!sessionStorage.getItem('rol') &&
      sessionStorage.getItem('cambioContrasenaRequerido') !== 'true' &&
      ruta !== '/login' &&
      ruta !== '/'
    );
  }
}
