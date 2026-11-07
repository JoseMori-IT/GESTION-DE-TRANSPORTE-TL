import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ReferenciaConsumo } from '../pages/reportes/consumo-calculo';
export interface ConfiguracionOperativa {
  version: number | null;
  sede: string;
  plantaBaseId: number | null;
  plantaBaseNombre: string | null;
  galonesPorKm: number;
  kgCo2PorKm: number;
  litrosPorGalon: number;
  unidadGalon: string;
  referenciaCombustible: string;
  referenciaEmisiones: string;
  actualizadoPor: string | null;
  actualizadoEn: string | null;
}
@Injectable({ providedIn: 'root' })
export class ConfiguracionOperativaService {
  private http = inject(HttpClient);
  private consulta = 0;
  readonly datos = signal<ConfiguracionOperativa | null>(null);
  readonly error = signal('');
  readonly cargando = signal(false);
  referencia(): ReferenciaConsumo | null {
    const c = this.datos();
    return c
      ? {
          version:
            c.version === null ? 'Valores iniciales del servidor' : 'Configuración v' + c.version,
          fecha: c.actualizadoEn ?? 'Inicial',
          combustible: 'Diésel',
          galonesPorKm: c.galonesPorKm,
          kgCo2PorKm: c.kgCo2PorKm,
          litrosPorGalon: c.litrosPorGalon,
          litrosPor100Km: c.galonesPorKm * c.litrosPorGalon * 100,
          fuente: c.referenciaCombustible + ' ' + c.referenciaEmisiones,
        }
      : null;
  }
  async cargar() {
    const n = ++this.consulta;
    this.cargando.set(true);
    this.error.set('');
    try {
      const c = await firstValueFrom(
        this.http.get<ConfiguracionOperativa>('/api/configuracion-operativa'),
      );
      if (n === this.consulta) this.datos.set(c);
    } catch {
      if (n === this.consulta) {
        this.datos.set(null);
        this.error.set(
          'No se pudo consultar la configuración operativa. Las estimaciones de combustible y CO₂ no están disponibles.',
        );
      }
    } finally {
      if (n === this.consulta) this.cargando.set(false);
    }
  }
  async guardar(c: ConfiguracionOperativa) {
    const actualizado = await firstValueFrom(
      this.http.put<ConfiguracionOperativa>('/api/configuracion-operativa', c),
    );
    ++this.consulta;
    this.datos.set(actualizado);
    this.error.set('');
    this.cargando.set(false);
    return actualizado;
  }
}
