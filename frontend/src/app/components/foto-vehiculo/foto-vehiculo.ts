import { Component, input, OnDestroy, OnInit, signal } from '@angular/core';

import { VehiculoService } from '../../services/vehiculo.service';

@Component({
  selector: 'app-foto-vehiculo',
  templateUrl: './foto-vehiculo.html',
  styleUrl: './foto-vehiculo.scss',
})
export class FotoVehiculoComponent implements OnInit, OnDestroy {
  readonly vehiculoId = input.required<number>();
  readonly tieneFoto = input(false);
  readonly placa = input('');
  readonly compacta = input(false);

  readonly urlFoto = signal<string | null>(null);
  readonly cargando = signal(false);
  private objectUrl: string | null = null;

  constructor(private readonly vehiculoService: VehiculoService) {}

  ngOnInit(): void {
    if (!this.tieneFoto()) {
      return;
    }

    this.cargando.set(true);

    this.vehiculoService.obtenerFoto(this.vehiculoId()).subscribe({
      next: (contenido) => {
        this.liberarUrl();
        this.objectUrl = URL.createObjectURL(contenido);
        this.urlFoto.set(this.objectUrl);
        this.cargando.set(false);
      },
      error: () => {
        this.urlFoto.set(null);
        this.cargando.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.liberarUrl();
  }

  private liberarUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
