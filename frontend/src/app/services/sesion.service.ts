import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AccesoOfflineService } from './acceso-offline.service';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SesionService {
  private readonly accesoOffline = inject(AccesoOfflineService);

  private readonly logoutUrl = '/api/auth/logout';

  private cerrandoSesion = false;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  cerrarSesion(): void {
    if (this.cerrandoSesion) {
      return;
    }

    this.cerrandoSesion = true;

    /*
     * Primero se comunica el cierre al backend para
     * registrar el evento en la auditoría.
     *
     * Aunque la petición falle, se elimina la sesión
     * local para no dejar al usuario dentro del sistema.
     */
    this.http
      .post<void>(this.logoutUrl, {})
      .pipe(
        finalize(() => {
          this.limpiarSesion();
          this.cerrandoSesion = false;
        }),
      )
      .subscribe({
        next: () => {
          // El cierre continúa dentro de finalize.
        },
        error: () => {
          // También se cerrará localmente desde finalize.
        },
      });
  }

  cerrarSesionLocal(): void {
    this.limpiarSesion();
  }

  private limpiarSesion(): void {
    this.accesoOffline.bloquear();
    sessionStorage.removeItem('accesoOffline');
    sessionStorage.removeItem('modoAccesoLocal');
    // Limpiar las copias de consulta; conservar la cola offline para su propietario.
    try {
      for (const clave of Object.keys(localStorage)) {
        if (clave.startsWith('viajes-chofer-')) localStorage.removeItem(clave);
      }
    } catch {
      /* La limpieza de la sesión debe continuar si el almacenamiento no está disponible. */
    }
    for (const clave of Object.keys(sessionStorage)) {
      if (clave.startsWith('transporte-borrador-')) sessionStorage.removeItem(clave);
    }
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('usuario');
    sessionStorage.removeItem('rol');
    sessionStorage.removeItem('conductorId');
    sessionStorage.removeItem('cambioContrasenaRequerido');
    sessionStorage.removeItem('usuarioId');
    sessionStorage.removeItem('nombres');
    sessionStorage.removeItem('correo');

    this.router.navigate(['/login'], {
      replaceUrl: true,
    });
  }
}
