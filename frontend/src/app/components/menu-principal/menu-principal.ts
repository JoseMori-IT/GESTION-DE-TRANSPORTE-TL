import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-menu-principal',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './menu-principal.html',
  styleUrl: './menu-principal.scss',
})
export class MenuPrincipal {
  readonly router = inject(Router);
  readonly sesion = inject(SesionService);
  readonly administrador = ['MASTER', 'COORDINADOR', 'ADMINISTRADOR'].includes(
    sessionStorage.getItem('rol') ?? '',
  );
  readonly esMaster = sessionStorage.getItem('rol') === 'MASTER';
  readonly esChofer = sessionStorage.getItem('rol') === 'CHOFER';
  readonly enlaces = this.esChofer
    ? [
        { ruta: '/chofer', nombre: 'Mis servicios' },
        { ruta: '/chofer/historial', nombre: 'Historial' },
      ]
    : [
        { ruta: '/dashboard', nombre: 'Resumen' },
        { ruta: '/vehiculos', nombre: 'Vehículos' },
        { ruta: '/flota', nombre: 'Flota' },
        { ruta: '/mantenimiento', nombre: 'Mantenimiento' },
        { ruta: '/conductores', nombre: 'Conductores' },
        { ruta: '/rutas', nombre: 'Rutas' },
        { ruta: '/solicitudes', nombre: 'Solicitudes' },
        { ruta: '/servicios', nombre: 'Servicios' },
        { ruta: '/reportes', nombre: 'Reportes' },
        { ruta: '/abastecimientos', nombre: 'Abastecimientos' },
      ];
}
