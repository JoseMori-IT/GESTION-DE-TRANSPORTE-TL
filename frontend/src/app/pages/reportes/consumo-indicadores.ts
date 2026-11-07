import { Component, input, computed } from '@angular/core';
import { calcularConsumo } from './consumo-calculo';
@Component({
  selector: 'app-consumo-indicadores',
  templateUrl: './consumo-indicadores.html',
  styleUrl: './consumo-indicadores.scss',
})
export class ConsumoIndicadores {
  readonly datos = input.required<ReturnType<typeof calcularConsumo>>();
  readonly meses = computed(() => this.datos().porMes.slice(-12));
  readonly unidades = computed(() => this.datos().porUnidad.slice(0, 8));
  numero(n: number | null, decimales = 1) {
    return n === null
      ? 'Sin datos'
      : n.toLocaleString('es-PE', { maximumFractionDigits: decimales });
  }
  ancho(n: number, filas: { galones: number }[]) {
    return (n / Math.max(1, ...filas.map((d) => d.galones))) * 100;
  }
  mes(clave: string) {
    return clave === 'SIN_FECHA' ? 'Sin fecha' : clave.slice(5, 7) + '/' + clave.slice(0, 4);
  }
}
