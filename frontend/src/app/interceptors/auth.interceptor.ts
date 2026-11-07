import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { apiDesdeBase } from './api-base';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const documento = inject(DOCUMENT);
  const token = sessionStorage.getItem('token');

  const esApiLocal = request.url.startsWith('/api/');

  const esLogin = request.url.includes('/api/auth/login');
  request = request.clone({ url: apiDesdeBase(request.url, documento.baseURI) });
  if (sessionStorage.getItem('accesoOffline') === 'true' && esApiLocal && !esLogin) {
    return throwError(
      () =>
        new HttpErrorResponse({
          status: 0,
          statusText: 'Acceso local: autentícate en la red corporativa para sincronizar',
        }),
    );
  }

  if (!token || !esApiLocal || esLogin) {
    return next(request);
  }

  const requestAutenticada = request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(requestAutenticada).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 403 && error.error?.codigo === 'CAMBIO_CONTRASENA_REQUERIDO') {
        sessionStorage.setItem('cambioContrasenaRequerido', 'true');
        router.navigate(['/cambiar-contrasena']);
      }
      if (error.status === 401) {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('cambioContrasenaRequerido');
        router.navigate(['/login']);
      }

      return throwError(() => error);
    }),
  );
};
