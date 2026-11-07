import { SesionActivaService } from './services/sesion-activa.service';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: SesionActivaService, useValue: {} }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('no muestra navegación privada sin una sesión', async () => {
    sessionStorage.clear();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-menu-principal')).toBeNull();
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
