import { Component, input } from '@angular/core';
import { ConsumoIndicadores } from './consumo-indicadores';
import { calcularConsumo } from './consumo-calculo';
@Component({
  selector: 'app-reporte-consumo-impreso',
  imports: [ConsumoIndicadores],
  templateUrl: './reporte-consumo-impreso.html',
  styleUrl: './reporte-general-impreso.scss',
})
export class ReporteConsumoImpreso {
  readonly datos = input.required<ReturnType<typeof calcularConsumo>>();
  readonly periodo = input('Todos los períodos');
  readonly ruta = input('TODAS');
  readonly generadoPor = input('Usuario');
  readonly fecha = input('');
  fila(id: number) {
    return this.datos().filas.find((f) => f.s.id === id);
  }
  numero(n: number | null, decimales = 1) {
    return n === null
      ? 'Sin datos'
      : n.toLocaleString('es-PE', { maximumFractionDigits: decimales });
  }
  fechaServicio(valor: string | null) {
    if (!valor) return 'Sin registro';
    const d = new Date(valor);
    return Number.isFinite(d.getTime())
      ? d.toLocaleString('es-PE', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        })
      : 'Sin registro';
  }
}
