import { Injectable, inject, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
@Injectable({ providedIn: 'root' })
export class SesionActivaService {
  private http = inject(HttpClient);
  private destroy = inject(DestroyRef);
  private ultimaActividad = Date.now();
  private renovando = false;
  private ultimoIntento = 0;
  constructor() {
    const actividad = () => {
      this.ultimaActividad = Date.now();
    };
    for (const evento of ['pointerdown', 'keydown', 'input', 'change'])
      window.addEventListener(evento, actividad, { passive: true });
    const timer = window.setInterval(() => void this.comprobar(), 30000);
    this.destroy.onDestroy(() => {
      window.clearInterval(timer);
      for (const evento of ['pointerdown', 'keydown', 'input', 'change'])
        window.removeEventListener(evento, actividad);
    });
  }
  async comprobar() {
    const ahora = Date.now();
    const token = sessionStorage.getItem('token');
    if (
      !token ||
      this.renovando ||
      document.hidden ||
      sessionStorage.getItem('accesoOffline') === 'true' ||
      sessionStorage.getItem('cambioContrasenaRequerido') === 'true' ||
      ahora - this.ultimaActividad > 5 * 60000 ||
      ahora - this.ultimoIntento < 60000
    )
      return;
    let exp: number;
    try {
      exp = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).exp * 1000;
    } catch {
      return;
    }
    if (!Number.isFinite(exp) || exp <= ahora || exp - ahora > 3 * 60000) return;
    this.ultimoIntento = ahora;
    this.renovando = true;
    try {
      const r = await firstValueFrom(this.http.post<{ token: string }>('/api/auth/renovar', {}));
      if (sessionStorage.getItem('token') === token && r.token)
        sessionStorage.setItem('token', r.token);
    } catch {
      /* No se prolonga una sesión si el servidor no confirma la renovación. */
    } finally {
      this.renovando = false;
    }
  }
}
