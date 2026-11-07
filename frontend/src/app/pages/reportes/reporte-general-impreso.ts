import { Component, input } from '@angular/core';
import { Indicadores } from './indicadores';
import { computed } from '@angular/core';
@Component({
  selector: 'app-reporte-general-impreso',
  imports: [],
  templateUrl: './reporte-general-impreso.html',
  styleUrl: './reporte-general-impreso.scss',
})
export class ReporteGeneralImpreso {
  readonly indicadores = input.required<Indicadores>();
  readonly periodo = input('Todos los períodos');
  readonly ruta = input('TODAS');
  readonly estado = input('TODOS');
  readonly generadoPor = input('Usuario');
  readonly fecha = input('');
  porcentaje(n: number, total: number) {
    return total ? Math.round((n / total) * 100) : 0;
  }
  ancho(n: number, filas: { valor: number }[]) {
    return (n / Math.max(1, ...filas.map((f) => f.valor))) * 100;
  }
  readonly odometrosRevision = computed(
    () =>
      this.indicadores()
        .datos()
        .detalles.filter((d) => d.distancia.fuente === 'REVISION').length,
  );
}
