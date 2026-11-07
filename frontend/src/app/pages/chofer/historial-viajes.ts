import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
import { fechaViaje, fechaHoraViaje, nombreProducto } from '../../services/presentacion-viaje';
import { IncidenciaViajeComponent } from '../../components/incidencia-viaje';
import { AvanceViajeComponent } from '../../components/avance-viaje';

@Component({
  selector: 'app-historial-viajes',
  imports: [IncidenciaViajeComponent, AvanceViajeComponent],
  templateUrl: './historial-viajes.html',
  styleUrl: './historial-viajes.scss',
})
export class HistorialViajesComponent {
  @Input({ required: true }) viajes: ServicioTransporte[] = [];
  @Output() readonly evidencias = new EventEmitter<ServicioTransporte>();
  @Output() readonly actualizado = new EventEmitter<ServicioTransporte>();
  readonly expandidoId = signal<number | null>(null);
  readonly fechaViaje = fechaViaje;
  readonly fechaHoraViaje = fechaHoraViaje;
  readonly nombreProducto = nombreProducto;
}
