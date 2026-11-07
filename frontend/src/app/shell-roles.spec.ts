import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { afterEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { MenuPrincipal } from './components/menu-principal/menu-principal';
import { SesionService } from './services/sesion.service';

describe('Menú compartido por rol', () => {
  afterEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });
  function preparar(rol: string, ruta: string, obligatorio = false): void {
    sessionStorage.setItem('token', 'sesion-de-prueba');
    sessionStorage.setItem('rol', rol);
    sessionStorage.setItem('cambioContrasenaRequerido', String(obligatorio));
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { url: ruta, events: new Subject() } },
        { provide: SesionService, useValue: {} },
      ],
    });
  }
  it('muestra el menú al chofer con su único módulo permitido', () => {
    preparar('CHOFER', '/chofer');
    const app = TestBed.runInInjectionContext(() => new App());
    const menu = TestBed.runInInjectionContext(() => new MenuPrincipal());
    expect(app.mostrarMenu()).toBe(true);
    expect(menu.enlaces.map((e) => e.ruta)).toEqual(['/chofer', '/chofer/historial']);
    expect(menu.administrador).toBe(false);
    expect(menu.esMaster).toBe(false);
  });
  it('conserva los módulos administrativos para el administrador', () => {
    preparar('ADMINISTRADOR', '/dashboard');
    const menu = TestBed.runInInjectionContext(() => new MenuPrincipal());
    expect(menu.enlaces.some((e) => e.ruta === '/conductores')).toBe(true);
    expect(menu.administrador).toBe(true);
  });
  it('mantiene oculto el menú durante el cambio obligatorio y en login', () => {
    preparar('CHOFER', '/cambiar-contrasena', true);
    expect(TestBed.runInInjectionContext(() => new App()).mostrarMenu()).toBe(false);
    TestBed.resetTestingModule();
    preparar('CHOFER', '/login');
    expect(TestBed.runInInjectionContext(() => new App()).mostrarMenu()).toBe(false);
  });
});
