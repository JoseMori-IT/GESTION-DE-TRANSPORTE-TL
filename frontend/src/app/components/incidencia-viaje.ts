import { Component, input, output, computed, signal, inject, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ServicioTransporte } from '../services/servicio-transporte.service';
import { firstValueFrom } from 'rxjs';
import { FotosOfflineService } from '../services/fotos-offline.service';
@Component({
  selector: 'app-incidencia-viaje',
  imports: [FormsModule],
  templateUrl: './incidencia-viaje.html',
  styleUrl: './incidencia-viaje.scss',
})
export class IncidenciaViajeComponent {
  readonly servicio = input.required<ServicioTransporte>();
  readonly actualizado = output<ServicioTransporte>();
  readonly chofer = input(false);
  readonly activa = computed(() =>
    this.servicio().incidencias?.find(
      (i) => !['RESUELTA', 'RETORNO_CONFIRMADO'].includes(i.estado),
    ),
  );
  readonly obteniendoGps = signal(false);
  readonly modo = signal('');
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly vehiculos = signal<any[]>([]);
  readonly conductores = signal<any[]>([]);
  readonly puedeGestionar = ['MASTER', 'COORDINADOR', 'ADMINISTRADOR', 'OPERADOR'].includes(
    sessionStorage.getItem('rol') || '',
  );
  datos = {
    descripcion: '',
    lugar: '',
    tecnico: '',
    diagnostico: '',
    vehiculoAuxilioId: null as number | null,
    conductorAuxilioId: null as number | null,
    odometroRegresoKm: null as number | null,
  };
  latitud: number | null = null;
  longitud: number | null = null;
  private destroy = inject(DestroyRef);
  constructor() {
    const timer = window.setInterval(() => {
      if (this.chofer() && this.activa() && !this.ocupado()) void this.refrescar(true);
    }, 30000);
    this.destroy.onDestroy(() => window.clearInterval(timer));
  }
  async refrescar(silencioso = false) {
    if (this.ocupado()) return;
    try {
      const s = this.chofer()
        ? (await firstValueFrom(this.http.get<ServicioTransporte[]>('/api/chofer/servicios'))).find(
            (s) => s.id === this.servicio().id,
          )
        : await firstValueFrom(
            this.http.get<ServicioTransporte>('/api/servicios/' + this.servicio().id),
          );
      if (s) {
        this.actualizado.emit(s);
        if (!silencioso) this.error.set('');
      }
    } catch {
      if (!silencioso)
        this.error.set('No se pudo consultar la atención. Reintenta cuando tengas conexión.');
    }
  }
  private http = inject(HttpClient);
  private fotos = inject(FotosOfflineService);
  recepcionConfirmada = false;
  async elegir(accion: string) {
    this.error.set('');
    this.recepcionConfirmada = false;
    this.datos.diagnostico = '';
    this.modo.set(accion);
  }
  ubicacion() {
    if (this.obteniendoGps()) return;
    this.error.set('');
    if (!navigator.geolocation) {
      this.error.set('Este navegador no permite obtener el GPS. Escribe un punto de referencia.');
      return;
    }
    this.obteniendoGps.set(true);
    navigator.geolocation.getCurrentPosition(
      (p) => {
        this.latitud = p.coords.latitude;
        this.longitud = p.coords.longitude;
        this.obteniendoGps.set(false);
      },
      (e) => {
        this.obteniendoGps.set(false);
        this.error.set(
          e.code === 1
            ? 'Permite el acceso a tu ubicación en el navegador o escribe un punto de referencia.'
            : e.code === 3
              ? 'La ubicación está tardando demasiado. Puedes reintentar o escribir un punto de referencia.'
              : 'No se pudo obtener el GPS. Escribe un punto de referencia.',
        );
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }

  async enviar(accion: string) {
    if (this.ocupado() || (accion === 'RETORNO_PLANTA' && !this.recepcionConfirmada)) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      const i = this.activa();
      const s = await firstValueFrom(
        this.http.post<ServicioTransporte>('/api/incidencias-viaje/' + this.servicio().id, {
          accion,
          ...this.datos,
          latitud: this.latitud,
          longitud: this.longitud,
          incidenciaId: i?.id ?? null,
          version: i?.versionRegistro ?? null,
        }),
      );
      this.actualizado.emit(s);
      this.modo.set('');
    } catch (e: any) {
      this.error.set(
        e.status === 0
          ? 'No hay conexión con el servidor. El reporte aún no se envió. Contacta al coordinador por el medio disponible y vuelve a intentarlo; el texto se conserva mientras mantengas esta vista abierta.'
          : e.error?.mensaje ||
              e.error?.detail ||
              e.error?.message ||
              'No se pudo registrar. Actualiza el viaje y vuelve a intentarlo.',
      );
    } finally {
      this.ocupado.set(false);
    }
  }
  nombre(estado: string) {
    return (
      (
        {
          REPORTADA: 'Avería reportada · esperando atención',
          TECNICO_EN_CAMINO: 'Técnico asignado',
          LISTA_CONTINUAR: 'Reparación registrada · pendiente de reanudación',
          REMOLQUE_ASIGNADO: 'Auxilio asignado · regreso a planta',
          RESUELTA: 'Reparada · viaje reanudado',
          RETORNO_CONFIRMADO: 'Retorno a planta confirmado',
        } as Record<string, string>
      )[estado] || estado
    );
  }
  siguientePaso(estado: string) {
    return (
      (
        {
          REPORTADA: this.chofer()
            ? 'Tu viaje está detenido. Comunícate con el coordinador para recibir indicaciones.'
            : 'Si la unidad ya fue reparada, registra el trabajo realizado. Si regresó a planta, confirma su recepción.',
          TECNICO_EN_CAMINO: this.chofer()
            ? 'Espera que el coordinador registre la reparación antes de reanudar.'
            : 'Registra la reparación o confirma el retorno cuando la unidad llegue a planta.',
          LISTA_CONTINUAR: this.chofer()
            ? 'Confirma cuando vayas a retomar el recorrido. Tus paradas pendientes se conservan.'
            : 'El chofer puede reanudar desde su panel. También puedes confirmar su reanudación previa coordinación o registrar que regresó a planta.',
          REMOLQUE_ASIGNADO:
            'Existe un auxilio registrado en una versión anterior. Su historial se conserva; el coordinador puede confirmar la recepción de la unidad en planta.',
        } as Record<string, string>
      )[estado] || 'Consulta el historial de atención.'
    );
  }
  etiquetaConfirmar() {
    return (
      (
        {
          REPORTAR: 'Enviar reporte',
          TECNICO: 'Guardar técnico',
          REPARADA: 'Guardar reparación',
          CONTINUAR: 'Confirmar reanudación',
          RETORNO_PLANTA: 'Confirmar recepción en planta',
        } as Record<string, string>
      )[this.modo()] || 'Confirmar'
    );
  }
  fecha(valor: string | null) {
    return valor ? new Date(valor).toLocaleString('es-PE') : 'Pendiente';
  }
}
