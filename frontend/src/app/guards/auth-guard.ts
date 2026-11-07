import { inject } from '@angular/core';
import { AccesoOfflineService } from '../services/acceso-offline.service';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  const token = sessionStorage.getItem('token');
  const rol = sessionStorage.getItem('rol');
  if (sessionStorage.getItem('accesoOffline') === 'true' && rol === 'CHOFER') {
    try {
      inject(AccesoOfflineService).validarSesionLocal();
    } catch {
      return router.createUrlTree(['/login']);
    }
    return state.url === '/chofer' || state.url === '/chofer/historial'
      ? true
      : router.createUrlTree(['/chofer']);
  }

  if (!token || !rol) {
    sessionStorage.clear();

    return router.createUrlTree(['/login']);
  }

  /*
   * El chofer solamente puede ingresar a su panel.
   */
  if (state.url.startsWith('/cambiar-contrasena')) {
    return true;
  }

  if (sessionStorage.getItem('cambioContrasenaRequerido') === 'true') {
    return router.createUrlTree(['/cambiar-contrasena']);
  }

  if (
    (state.url.startsWith('/restablecer-cuentas') ||
      state.url.startsWith('/configuracion-operativa')) &&
    !['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(rol)
  ) {
    return router.createUrlTree([rol === 'CHOFER' ? '/chofer' : '/dashboard']);
  }

  if (state.url.startsWith('/auditoria') && rol !== 'MASTER') {
    return router.createUrlTree([rol === 'CHOFER' ? '/chofer' : '/dashboard']);
  }

  if (rol === 'CHOFER') {
    if (state.url.startsWith('/chofer')) {
      return true;
    }

    return router.createUrlTree(['/chofer']);
  }

  /*
   * Los usuarios administrativos no deben ingresar
   * al panel exclusivo del chofer.
   */
  if (state.url.startsWith('/chofer')) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
