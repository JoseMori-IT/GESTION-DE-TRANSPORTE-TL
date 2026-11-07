import { Abastecimientos } from './pages/abastecimientos/abastecimientos';
import { Routes } from '@angular/router';
import { CambiarContrasenaPage } from './pages/cambiar-contrasena/cambiar-contrasena';
import { RestablecerCuentasPage } from './pages/restablecer-cuentas/restablecer-cuentas';

import { authGuard } from './guards/auth-guard';

import { Chofer } from './pages/chofer/chofer';
import { Conductores } from './pages/conductores/conductores';
import { Dashboard } from './pages/dashboard/dashboard';
import { Flota } from './pages/flota/flota';
import { Login } from './pages/login/login';
import { Mantenimiento } from './pages/mantenimiento/mantenimiento';
import { PersonalAdministrativoPage } from './pages/personal-administrativo/personal-administrativo';
import { Reportes } from './pages/reportes/reportes';
import { Rutas } from './pages/rutas/rutas';
import { Servicios } from './pages/servicios/servicios';
import { Solicitudes } from './pages/solicitudes/solicitudes';
import { Vehiculos } from './pages/vehiculos/vehiculos';
import { AuditoriaPage } from './pages/auditoria/auditoria';

export const routes: Routes = [
  { path: 'configuracion-operativa', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'abastecimientos', component: Abastecimientos, canActivate: [authGuard] },
  { path: 'cambiar-contrasena', component: CambiarContrasenaPage, canActivate: [authGuard] },
  { path: 'restablecer-cuentas', component: RestablecerCuentasPage, canActivate: [authGuard] },
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard],
  },
  {
    path: 'flota',
    component: Flota,
    canActivate: [authGuard],
  },
  {
    path: 'vehiculos',
    component: Vehiculos,
    canActivate: [authGuard],
  },
  {
    path: 'conductores',
    component: Conductores,
    canActivate: [authGuard],
  },
  {
    path: 'rutas',
    component: Rutas,
    canActivate: [authGuard],
  },
  {
    path: 'servicios',
    component: Servicios,
    canActivate: [authGuard],
  },
  {
    path: 'chofer/historial',
    component: Chofer,
    canActivate: [authGuard],
  },
  {
    path: 'chofer',
    component: Chofer,
    canActivate: [authGuard],
  },
  {
    path: 'reportes',
    component: Reportes,
    canActivate: [authGuard],
  },
  {
    path: 'solicitudes',
    component: Solicitudes,
    canActivate: [authGuard],
  },
  {
    path: 'mantenimiento',
    component: Mantenimiento,
    canActivate: [authGuard],
  },
  {
    path: 'personal-administrativo',
    component: PersonalAdministrativoPage,
    canActivate: [authGuard],
  },

  {
    path: 'auditoria',
    component: AuditoriaPage,
    canActivate: [authGuard],
  },

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
