import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { Servicios } from './servicios';
import {
  ServicioTransporteService,
  ServicioTransporte,
} from '../../services/servicio-transporte.service';
import { EvidenciaServicioService } from '../../services/evidencia-servicio.service';
import { EncuestaServicioService } from '../../services/encuesta-servicio.service';
import { SesionService } from '../../services/sesion.service';
import { DialogosService } from '../../services/dialogos.service';
import { EMPTY } from 'rxjs';
describe('Evidencias según estado del servicio', () => {
  afterEach(() => TestBed.resetTestingModule());
  for (const estado of ['PROGRAMADO', 'EN_CURSO', 'COMPLETADO', 'CANCELADO'] as const) {
    it(`formulario disponible solamente en curso: ${estado}`, () => {
      const registrar = vi.fn();
      TestBed.configureTestingModule({
        imports: [Servicios],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: { events: EMPTY } },
          { provide: ServicioTransporteService, useValue: { listar: () => EMPTY } },
          { provide: EvidenciaServicioService, useValue: { registrar } },
          { provide: EncuestaServicioService, useValue: {} },
          { provide: SesionService, useValue: {} },
          { provide: DialogosService, useValue: {} },
        ],
      });
      const fixture = TestBed.createComponent(Servicios);
      const pagina = fixture.componentInstance;
      vi.spyOn(pagina, 'ngOnInit').mockImplementation(() => {});
      pagina.servicioSeleccionado.set({
        id: 1,
        codigo: 'SER-001',
        estado,
        vehiculo: { placa: 'ABC-123' },
        ruta: { nombre: 'Ruta' },
      } as ServicioTransporte);
      pagina.mostrarEvidencias.set(true);
      fixture.detectChanges();
      const modal = fixture.nativeElement as HTMLElement;
      expect(!!modal.querySelector('.evidence-form')).toBe(estado === 'EN_CURSO');
      expect(!!modal.querySelector('.evidence-readonly')).toBe(estado !== 'EN_CURSO');
      if (estado !== 'EN_CURSO') {
        pagina.registrarEvidencia();
        expect(registrar).not.toHaveBeenCalled();
        expect(pagina.mensajeEvidencia()).toContain('Registro cerrado');
      }
    });
  }
});
